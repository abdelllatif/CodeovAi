package codeovai.codeovai.service.visual;

import codeovai.codeovai.model.DiagramElement;
import codeovai.codeovai.model.DiagramRelationship;
import codeovai.codeovai.service.core.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.io.MemoryUsageSetting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisualInterpretationService {

    private final LoggingService loggingService;
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "class\\s+(\\w+)\\s*\\{([^}]*)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(\\w+)\\s*\\(([^)]*)\\)\\s*[:]?\\s*(\\w+)?");
    private static final Pattern RELATIONSHIP_PATTERN = Pattern.compile(
        "(\\w+)\\s*[-=]\\s*(\\w+)\\s*\\[(.*?)\\]");
    private static final Pattern STEREOTYPE_PATTERN = Pattern.compile(
        "«(.*?)»\\s*(\\w+)");
    
    public List<DiagramElement> interpretDiagram(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("VISUAL_INTERPRETATION", "Starting diagram interpretation", 
            "filename", file.getOriginalFilename(), 
            "size",  String.valueOf(file.getSize()));
        
        try {
            String extractedText = extractText(file);
            List<DiagramElement> elements = parseDiagramElements(extractedText);
            
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("VISUAL_INTERPRETATION", "complete_interpretation", duration, 
                "elements", String.valueOf(elements.size()));
            
            return elements;
        } catch (Exception e) {
            loggingService.logError("VISUAL_INTERPRETATION", "Diagram interpretation failed", e);
            throw new RuntimeException("Failed to interpret diagram", e);
        }
    }
    
    private String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename().toLowerCase();
        
        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || 
                   filename.endsWith(".png") || filename.endsWith(".gif") || 
                   filename.endsWith(".bmp") || filename.endsWith(".tiff")) {
            return extractFromImage(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type for visual interpretation");
        }
    }
    
    private String extractFromPdf(MultipartFile file) throws IOException {
        StringBuilder allText = new StringBuilder();
        try (PDDocument document = PDDocument.load(
                new ByteArrayInputStream(file.getBytes()), MemoryUsageSetting.setupMainMemoryOnly())) {
            PDFRenderer renderer = new PDFRenderer(document);

            
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String pageText = performOcr(image);
                allText.append(pageText).append("\n--- PAGE ").append(page + 1).append(" ---\n");
            }
        }
        
        return allText.toString();
    }
    
    private String extractFromImage(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        return performOcr(image);
    }
    
    private String performOcr(BufferedImage image) {
        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata"); // Ensure tessdata directory is available
            tesseract.setLanguage("eng");
            
            String result = tesseract.doOCR(image);
            loggingService.logPipelineStep("OCR", "Text extraction completed", 
                "textLength", String.valueOf(result.length()));
            
            return result;
        } catch (Exception e) {
            loggingService.logError("OCR", "OCR processing failed", e);
            throw new RuntimeException("Failed to perform OCR", e);
        }
    }
    
    private List<DiagramElement> parseDiagramElements(String text) {
        List<DiagramElement> elements = new ArrayList<>();
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            Matcher stereotypeMatcher = STEREOTYPE_PATTERN.matcher(line);
            if (stereotypeMatcher.find()) {
                String stereotype = stereotypeMatcher.group(1);
                String className = stereotypeMatcher.group(2);
                
                DiagramElement element = new DiagramElement();
                element.setName(className);
                element.setType(determineTypeFromStereotype(stereotype));
                element.setStereotypes(List.of(stereotype));
                
                List<String> methods = new ArrayList<>();
                List<String> attributes = new ArrayList<>();
                
                for (int i = getCurrentLineIndex(lines, line) + 1; i < lines.length && i < getCurrentLineIndex(lines, line) + 10; i++) {
                    String nextLine = lines[i].trim();
                    
                    if (nextLine.contains("(") && nextLine.contains(")")) {
                        Matcher methodMatcher = METHOD_PATTERN.matcher(nextLine);
                        if (methodMatcher.find()) {
                            methods.add(methodMatcher.group());
                        }
                    } else if (nextLine.contains(":") && !nextLine.contains("(")) {
                        attributes.add(nextLine);
                    }
                }
                
                element.setMethods(methods);
                element.setAttributes(attributes);
                elements.add(element);
            }
            
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                String classBody = classMatcher.group(2);
                
                DiagramElement element = new DiagramElement();
                element.setName(className);
                element.setType("CLASS");
                
                List<String> methods = new ArrayList<>();
                List<String> attributes = new ArrayList<>();
                
                String[] bodyLines = classBody.split(";");
                for (String bodyLine : bodyLines) {
                    bodyLine = bodyLine.trim();
                    if (bodyLine.contains("(") && bodyLine.contains(")")) {
                        methods.add(bodyLine);
                    } else if (!bodyLine.isEmpty()) {
                        attributes.add(bodyLine);
                    }
                }
                
                element.setMethods(methods);
                element.setAttributes(attributes);
                elements.add(element);
            }
            
            // Parse relationships
            Matcher relationshipMatcher = RELATIONSHIP_PATTERN.matcher(line);
            if (relationshipMatcher.find()) {
                String from = relationshipMatcher.group(1);
                String to = relationshipMatcher.group(2);
                String relationshipInfo = relationshipMatcher.group(3);
                
                // Find or create the from element
                DiagramElement fromElement = findOrCreateElement(elements, from);
                
                // Parse relationship
                DiagramRelationship relationship = new DiagramRelationship();
                relationship.setFrom(from);
                relationship.setTo(to);
                relationship.setType(determineRelationshipType(relationshipInfo));
                relationship.setCardinality(extractCardinality(relationshipInfo));
                
                if (fromElement.getRelationships() == null) {
                    fromElement.setRelationships(new ArrayList<>());
                }
                fromElement.getRelationships().add(relationship);
                
                if (!elements.contains(fromElement)) {
                    elements.add(fromElement);
                }
            }
        }
        
        // Post-processing: enhance element detection
        enhanceElementDetection(elements, text);
        
        return elements;
    }
    
    private String determineTypeFromStereotype(String stereotype) {
        String lowerStereo = stereotype.toLowerCase();
        if (lowerStereo.contains("controller") || lowerStereo.contains("rest")) {
            return "CONTROLLER";
        } else if (lowerStereo.contains("service") || lowerStereo.contains("business")) {
            return "SERVICE";
        } else if (lowerStereo.contains("repository") || lowerStereo.contains("dao")) {
            return "REPOSITORY";
        } else if (lowerStereo.contains("entity") || lowerStereo.contains("model")) {
            return "ENTITY";
        } else if (lowerStereo.contains("component") || lowerStereo.contains("module")) {
            return "COMPONENT";
        }
        return "CLASS";
    }
    
    private String determineRelationshipType(String relationshipInfo) {
        String lower = relationshipInfo.toLowerCase();
        if (lower.contains("extends") || lower.contains("inherit")) {
            return "INHERITANCE";
        } else if (lower.contains("compose") || lower.contains("owns")) {
            return "COMPOSITION";
        } else if (lower.contains("aggregate") || lower.contains("has")) {
            return "AGGREGATION";
        } else if (lower.contains("use") || lower.contains("depend")) {
            return "DEPENDENCY";
        }
        return "ASSOCIATION";
    }
    
    private String extractCardinality(String relationshipInfo) {
        // Look for cardinality patterns like 1, *, 0..1, 1..*
        Pattern cardinalityPattern = Pattern.compile("(\\d+|\\*|\\d+\\.\\.\\d+)");
        Matcher matcher = cardinalityPattern.matcher(relationshipInfo);
        return matcher.find() ? matcher.group(1) : "1";
    }
    
    private DiagramElement findOrCreateElement(List<DiagramElement> elements, String name) {
        return elements.stream()
            .filter(e -> name.equals(e.getName()))
            .findFirst()
            .orElseGet(() -> {
                DiagramElement element = new DiagramElement();
                element.setName(name);
                element.setType("CLASS");
                return element;
            });
    }
    
    private int getCurrentLineIndex(String[] lines, String targetLine) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].equals(targetLine)) {
                return i;
            }
        }
        return -1;
    }
    
    private void enhanceElementDetection(List<DiagramElement> elements, String fullText) {
        // Additional processing to detect missed elements
        String[] lines = fullText.split("\n");

        for (String line : lines) {
            line = line.trim();

            final String currentLine = line;
            if (currentLine.matches("^[A-Z][a-zA-Z0-9_]*$") && currentLine.length() > 2) {
                boolean exists = elements.stream()
                        .anyMatch(e -> currentLine.equals(e.getName()));

                if (!exists) {
                    DiagramElement element = new DiagramElement();
                    element.setName(currentLine);
                    element.setType("CLASS");
                    elements.add(element);
                }
            }
        }

        
        // Detect stereotypes in the full text that might have been missed
        Pattern globalStereotypePattern = Pattern.compile("«(.*?)»");
        Matcher matcher = globalStereotypePattern.matcher(fullText);
        
        while (matcher.find()) {
            String stereotype = matcher.group(1);
            String context = fullText.substring(Math.max(0, matcher.start() - 50), 
                                               Math.min(fullText.length(), matcher.end() + 50));
            
            // Try to find associated element
            for (DiagramElement element : elements) {
                if (context.contains(element.getName()) && 
                    (element.getStereotypes() == null || !element.getStereotypes().contains(stereotype))) {
                    
                    if (element.getStereotypes() == null) {
                        element.setStereotypes(new ArrayList<>());
                    }
                    element.getStereotypes().add(stereotype);
                    
                    // Update element type based on stereotype
                    element.setType(determineTypeFromStereotype(stereotype));
                    break;
                }
            }
        }
    }
}

package codeovai.codeovai.service.analyze;

import codeovai.codeovai.model.CodeElement;
import codeovai.codeovai.model.SourceFile;
import codeovai.codeovai.service.core.LoggingService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AstCodeScanningService {

    private final LoggingService loggingService;
    
    public List<CodeElement> scanProject(Path systemRoot) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("AST_SCANNING", "Starting AST-based project scanning", 
            "path", systemRoot.toString());
        
        try {
            List<SourceFile> sourceFiles = findSourceFiles(systemRoot);
            List<CodeElement> allElements = new ArrayList<>();
            
            for (SourceFile sourceFile : sourceFiles) {
                List<CodeElement> fileElements = scanFile(sourceFile, systemRoot);
                allElements.addAll(fileElements);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("AST_SCANNING", "complete_scan", duration, 
                "files", String.valueOf(sourceFiles.size()), 
                "elements", String.valueOf(allElements.size()));
            
            return allElements;
        } catch (Exception e) {
            loggingService.logError("AST_SCANNING", "Project scanning failed", e);
            throw new RuntimeException("Failed to scan project with AST", e);
        }
    }
    
    private List<SourceFile> findSourceFiles(Path systemRoot) throws IOException {
        List<SourceFile> sourceFiles = new ArrayList<>();
        
        Files.walk(systemRoot)
            .filter(Files::isRegularFile)
            .filter(this::isSupportedSourceFile)
            .filter(path -> !isIgnored(path))
            .forEach(path -> sourceFiles.add(classifyFile(path, systemRoot)));
        
        return sourceFiles;
    }
    
    private boolean isSupportedSourceFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".java") || fileName.endsWith(".kt") || 
               fileName.endsWith(".js") || fileName.endsWith(".ts") || 
               fileName.endsWith(".php");
    }
    
    private boolean isIgnored(Path path) {
        String pathString = path.toString().toLowerCase();
        return pathString.contains("target/") || pathString.contains("build/") || 
               pathString.contains("node_modules/") || pathString.contains(".git/") ||
               pathString.contains(".idea/") || pathString.contains("dist/") ||
               pathString.contains("out/");
    }
    
    private SourceFile classifyFile(Path filePath, Path root) {
        String relativePath = root.relativize(filePath).toString();
        String type = detectFileType(relativePath.toLowerCase());
        return new SourceFile(relativePath, type);
    }
    
    private String detectFileType(String path) {
        if (path.contains("controller")) return "CONTROLLER";
        if (path.contains("service")) return "SERVICE";
        if (path.contains("repository") || path.contains("dao")) return "REPOSITORY";
        if (path.contains("model") || path.contains("entity")) return "MODEL";
        if (path.contains("config")) return "CONFIG";
        return "OTHER";
    }
    
    private List<CodeElement> scanFile(SourceFile sourceFile, Path systemRoot) {
        List<CodeElement> elements = new ArrayList();
        Path filePath = systemRoot.resolve(sourceFile.getPath());
        
        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString().toLowerCase();
            
            if (fileName.endsWith(".java")) {
                elements.addAll(scanJavaFile(content, sourceFile.getPath()));
            } else if (fileName.endsWith(".kt")) {
                elements.addAll(scanKotlinFile(content, sourceFile.getPath()));
            } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) {
                elements.addAll(scanJavaScriptFile(content, sourceFile.getPath()));
            } else if (fileName.endsWith(".php")) {
                elements.addAll(scanPhpFile(content, sourceFile.getPath()));
            }
            
        } catch (IOException e) {
            loggingService.logError("AST_SCANNING", "Failed to read file: " + sourceFile.getPath(), e);
        }
        
        return elements;
    }
    
    private List<CodeElement> scanJavaFile(String content, String filePath) {
        List<CodeElement> elements = new ArrayList<>();
        
        try {
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> parseResult = parser.parse(content);
            
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                // Scan classes
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                    CodeElement element = createCodeElement(classDecl, "CLASS", filePath);
                    elements.add(element);
                });
                
                // Scan methods
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    CodeElement element = createCodeElement(method, "METHOD", filePath);
                    elements.add(element);
                });
                
                // Scan fields
                cu.findAll(FieldDeclaration.class).forEach(field -> {
                    CodeElement element = createCodeElement(field, "FIELD", filePath);
                    elements.add(element);
                });
                
                // Scan enums
                cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
                    CodeElement element = createCodeElement(enumDecl, "ENUM", filePath);
                    elements.add(element);
                });
            }
        } catch (Exception e) {
            loggingService.logError("AST_SCANNING", "Failed to parse Java file: " + filePath, e);
        }
        
        return elements;
    }
    
    private List<CodeElement> scanKotlinFile(String content, String filePath) {
        // Simplified Kotlin parsing - can be enhanced with a proper Kotlin parser
        List<CodeElement> elements = new ArrayList<>();
        
        // Basic regex-based parsing for Kotlin (fallback)
        if (content.contains("class ")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("class ") || line.startsWith("interface ") || line.startsWith("object ")) {
                    String name = extractNameFromDeclaration(line);
                    if (name != null) {
                        CodeElement element = new CodeElement();
                        element.setName(name);
                        element.setElementType(line.contains("interface") ? "INTERFACE" : "CLASS");
                        element.setSourceType("KOTLIN");
                        element.setFilePath(filePath);
                        elements.add(element);
                    }
                }
            }
        }
        
        return elements;
    }
    
    private List<CodeElement> scanJavaScriptFile(String content, String filePath) {
        // Simplified JavaScript/TypeScript parsing
        List<CodeElement> elements = new ArrayList<>();
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // Class detection
            if (line.startsWith("class ") || line.startsWith("export class ")) {
                String name = extractNameFromDeclaration(line);
                if (name != null) {
                    CodeElement element = new CodeElement();
                    element.setName(name);
                    element.setElementType("CLASS");
                    element.setSourceType(filePath.endsWith(".ts") ? "TYPESCRIPT" : "JAVASCRIPT");
                    element.setFilePath(filePath);
                    elements.add(element);
                }
            }
            
            // Function detection
            if (line.startsWith("function ") || line.startsWith("export function ") || 
                line.contains("= function(") || line.contains("= (") || line.contains("=>")) {
                String name = extractFunctionName(line);
                if (name != null) {
                    CodeElement element = new CodeElement();
                    element.setName(name);
                    element.setElementType("FUNCTION");
                    element.setSourceType(filePath.endsWith(".ts") ? "TYPESCRIPT" : "JAVASCRIPT");
                    element.setFilePath(filePath);
                    elements.add(element);
                }
            }
        }
        
        return elements;
    }
    
    private List<CodeElement> scanPhpFile(String content, String filePath) {
        // Simplified PHP parsing
        List<CodeElement> elements = new ArrayList<>();
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // Class detection
            if (line.startsWith("class ") || line.startsWith("interface ") || line.startsWith("trait ")) {
                String name = extractNameFromDeclaration(line);
                if (name != null) {
                    CodeElement element = new CodeElement();
                    element.setName(name);
                    element.setElementType(line.contains("interface") ? "INTERFACE" : 
                                          line.contains("trait") ? "TRAIT" : "CLASS");
                    element.setSourceType("PHP");
                    element.setFilePath(filePath);
                    elements.add(element);
                }
            }
            
            // Function detection
            if (line.startsWith("function ") || line.startsWith("public function ") ||
                line.startsWith("private function ") || line.startsWith("protected function ")) {
                String name = extractNameFromDeclaration(line);
                if (name != null) {
                    CodeElement element = new CodeElement();
                    element.setName(name);
                    element.setElementType("FUNCTION");
                    element.setSourceType("PHP");
                    element.setFilePath(filePath);
                    elements.add(element);
                }
            }
        }
        
        return elements;
    }
    
    private CodeElement createCodeElement(Node node, String elementType, String filePath) {
        CodeElement element = new CodeElement();
        element.setElementType(elementType);
        element.setFilePath(filePath);
        
        if (node instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) node;
            element.setName(classDecl.getNameAsString());
            element.setSourceType("JAVA");
            element.setVisibility(getVisibility(classDecl.getAccessSpecifier()));
            element.setAbstract(classDecl.isAbstract());
            element.setAnnotations(extractAnnotations(classDecl.getAnnotations()));
            element.setMethods(extractMethodNames(classDecl.getMethods()));
            
            if (classDecl.getExtendedTypes().isNonEmpty()) {
                element.setParentClass(classDecl.getExtendedTypes().get(0).getNameAsString());
            }
            
            element.setInterfaces(classDecl.getImplementedTypes().stream()
                .map(type -> type.getNameAsString())
                .collect(Collectors.toList()));
                
        } else if (node instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) node;
            element.setName(method.getNameAsString());
            element.setSourceType("JAVA");
            element.setVisibility(getVisibility(method.getAccessSpecifier()));
            element.setStatic(method.isStatic());
            element.setAnnotations(extractAnnotations(method.getAnnotations()));
            
            if (method.getType() != null) {
                element.setReturnType(method.getType().asString());
            }
            
            element.setParameters(method.getParameters().stream()
                .map(param -> param.getTypeAsString() + " " + param.getNameAsString())
                .collect(Collectors.toList()));
                
        } else if (node instanceof FieldDeclaration) {
            FieldDeclaration field = (FieldDeclaration) node;
            element.setName(field.getVariable(0).getNameAsString());
            element.setSourceType("JAVA");
            element.setVisibility(getVisibility(field.getAccessSpecifier()));
            element.setStatic(field.isStatic());
            
            if (field.getElementType() != null) {
                element.setReturnType(field.getElementType().asString());
            }
        }
        
        return element;
    }
    private String getVisibility(AccessSpecifier accessSpecifier) {
        if (accessSpecifier == null) return "package";
        return accessSpecifier.asString();
    }


    private List<String> extractAnnotations(List<AnnotationExpr> annotations) {
        return annotations.stream()
            .map(AnnotationExpr::getNameAsString)
            .collect(Collectors.toList());
    }
    
    private List<String> extractMethodNames(List<MethodDeclaration> methods) {
        return methods.stream()
            .map(MethodDeclaration::getNameAsString)
            .collect(Collectors.toList());
    }
    
    private String extractNameFromDeclaration(String line) {
        String[] parts = line.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("class") || parts[i].equals("interface") || 
                parts[i].equals("object") || parts[i].equals("trait") ||
                parts[i].equals("function")) {
                String name = parts[i + 1];
                // Remove any trailing characters like {, (, etc.
                name = name.replaceAll("[{(:].*$", "");
                return name.isEmpty() ? null : name;
            }
        }
        return null;
    }
    
    private String extractFunctionName(String line) {
        // Extract function name from various JavaScript patterns
        if (line.contains("function ")) {
            int index = line.indexOf("function ");
            String rest = line.substring(index + 9);
            return rest.split("[\\s(]")[0];
        } else if (line.contains("= function(")) {
            String name = line.split("=")[0].trim();
            return name.isEmpty() ? "anonymous" : name;
        } else if (line.contains("=>")) {
            String name = line.split("=>")[0].trim();
            // Handle arrow function patterns
            name = name.replaceAll("[\\s(].*$", "");
            return name.isEmpty() ? "arrow" : name;
        }
        return null;
    }
}

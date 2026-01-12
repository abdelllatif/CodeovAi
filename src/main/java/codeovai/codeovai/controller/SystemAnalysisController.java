package codeovai.codeovai.controller;

import codeovai.codeovai.enumeration.InputType;
import codeovai.codeovai.model.*;
import codeovai.codeovai.service.ai.EnhancedPromptBuilderService;
import codeovai.codeovai.service.ai.GeminiClientService;
import codeovai.codeovai.service.analyze.AstCodeScanningService;
import codeovai.codeovai.service.analyze.EnhancedSystemContextBuilderService;
import codeovai.codeovai.service.classification.InputClassifierService;
import codeovai.codeovai.service.core.CacheService;
import codeovai.codeovai.service.core.LoggingService;
import codeovai.codeovai.service.upload.SecureZipExtractionService;
import codeovai.codeovai.service.visual.VisualInterpretationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/codeovAi")
@RequiredArgsConstructor
@Slf4j
public class SystemAnalysisController {

    private final InputClassifierService inputClassifierService;
    private final SecureZipExtractionService secureZipExtractionService;
    private final AstCodeScanningService astCodeScanningService;
    private final VisualInterpretationService visualInterpretationService;
    private final EnhancedSystemContextBuilderService contextBuilderService;
    private final EnhancedPromptBuilderService promptBuilderService;
    private final GeminiClientService geminiClientService;
    private final CacheService cacheService;
    private final LoggingService loggingService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeFiles(@RequestParam("files") MultipartFile[] files) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("SYSTEM_ANALYSIS", "Starting file analysis", 
            "fileCount", String.valueOf(files.length));

        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("No files uploaded.");
            }

            List<CompletableFuture<AnalysisResult>> futures = new ArrayList<>();
            
            for (MultipartFile file : files) {
                CompletableFuture<AnalysisResult> future = CompletableFuture.supplyAsync(() -> 
                    analyzeSingleFile(file));
                futures.add(future);
            }

            // Wait for all analyses to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

            allFutures.join();

            // Collect results
            List<AnalysisResult> results = new ArrayList<>();
            for (CompletableFuture<AnalysisResult> future : futures) {
                results.add(future.get());
            }

            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("SYSTEM_ANALYSIS", "complete_analysis", duration, 
                "files", String.valueOf(files.length));

            if (results.size() == 1) {
                return ResponseEntity.ok(results.get(0));
            } else {
                // Merge multiple results
                AnalysisResponse mergedResponse = mergeResults(results);
                return ResponseEntity.ok(mergedResponse);
            }

        } catch (Exception e) {
            loggingService.logError("SYSTEM_ANALYSIS", "Analysis failed", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing files: " + e.getMessage());
        }
    }

    private AnalysisResult analyzeSingleFile(MultipartFile file) {
        loggingService.logPipelineStep("FILE_ANALYSIS", "Starting single file analysis", 
            "filename", file.getOriginalFilename());

        try {
            // Classify input type
            InputType inputType = inputClassifierService.classifyInput(file);
            
            if (!inputClassifierService.isSupportedType(inputType)) {
                throw new IllegalArgumentException("Unsupported file type: " + inputType);
            }

            // Process based on input type
            switch (inputType) {
                case ZIP_ARCHIVE:
                    return processZipFile(file);
                case PDF_DOCUMENT:
                case IMAGE_FILE:
                    return processVisualFile(file);
                case TEXT_FILE:
                    return processTextFile(file);
                default:
                    throw new IllegalArgumentException("Unsupported input type: " + inputType);
            }
        } catch (Exception e) {
            loggingService.logError("FILE_ANALYSIS", "Single file analysis failed", e);
            throw new RuntimeException("Failed to analyze file: " + file.getOriginalFilename(), e);
        }
    }

    private AnalysisResult processZipFile(MultipartFile file) {
        loggingService.logPipelineStep("ZIP_PROCESSING", "Processing ZIP archive", 
            "filename", file.getOriginalFilename());

        // Extract ZIP securely
        String systemId = secureZipExtractionService.extractSecurely(file);
        
        try {
            // Scan code with AST parsing
            List<CodeElement> codeElements = astCodeScanningService.scanProject(
                secureZipExtractionService.getSystemRoot(systemId));

            // Build context
            SystemContext systemContext = contextBuilderService.buildContextFromCode(codeElements);

            // Generate prompt
            String prompt = promptBuilderService.buildCodeAnalysisPrompt(codeElements);

            // Get AI explanation
            String explanation = geminiClientService.generateExplanation(prompt);

            return new AnalysisResult(
                file.getOriginalFilename(),
                InputType.ZIP_ARCHIVE,
                codeElements,
                null,
                systemContext,
                explanation,
                systemId
            );
        } finally {
            // Cleanup extracted files
            secureZipExtractionService.cleanupSystem(systemId);
        }
    }

    private AnalysisResult processVisualFile(MultipartFile file) {
        loggingService.logPipelineStep("VISUAL_PROCESSING", "Processing visual file", 
            "filename", file.getOriginalFilename());

        // Interpret diagram
        List<DiagramElement> diagramElements = visualInterpretationService.interpretDiagram(file);

        // Build context
        SystemContext systemContext = contextBuilderService.buildContextFromDiagram(diagramElements);

        // Generate prompt
        String prompt = promptBuilderService.buildDiagramAnalysisPrompt(diagramElements);

        // Get AI explanation
        String explanation = geminiClientService.generateExplanation(prompt);

        return new AnalysisResult(
            file.getOriginalFilename(),
            inputClassifierService.classifyInput(file),
            null,
            diagramElements,
            systemContext,
            explanation,
            null
        );
    }

    private AnalysisResult processTextFile(MultipartFile file) {
        loggingService.logPipelineStep("TEXT_PROCESSING", "Processing text file", 
            "filename", file.getOriginalFilename());

        // For now, treat text files as simple content
        // This can be enhanced later based on specific requirements
        return new AnalysisResult(
            file.getOriginalFilename(),
            InputType.TEXT_FILE,
            null,
            null,
            null,
            "Text file processing not yet implemented - file: " + file.getOriginalFilename(),
            null
        );
    }

    private AnalysisResponse mergeResults(List<AnalysisResult> results) {
        loggingService.logPipelineStep("RESULT_MERGING", "Merging multiple analysis results", 
            "resultCount", String.valueOf(results.size()));

        List<CodeElement> allCodeElements = new ArrayList<>();
        List<DiagramElement> allDiagramElements = new ArrayList<>();
        StringBuilder mergedExplanation = new StringBuilder();
        List<String> fileNames = new ArrayList<>();

        for (AnalysisResult result : results) {
            fileNames.add(result.getFileName());
            
            if (result.getCodeElements() != null) {
                allCodeElements.addAll(result.getCodeElements());
            }
            
            if (result.getDiagramElements() != null) {
                allDiagramElements.addAll(result.getDiagramElements());
            }
            
            if (result.getExplanation() != null) {
                mergedExplanation.append("=== File: ").append(result.getFileName()).append(" ===\n");
                mergedExplanation.append(result.getExplanation()).append("\n\n");
            }
        }

        // Build merged context if we have elements
        SystemContext mergedContext = null;
        if (!allCodeElements.isEmpty()) {
            mergedContext = contextBuilderService.buildContextFromCode(allCodeElements);
        } else if (!allDiagramElements.isEmpty()) {
            mergedContext = contextBuilderService.buildContextFromDiagram(allDiagramElements);
        }

        // Generate merged explanation if we have context
        if (mergedContext != null) {
            String mergedPrompt = promptBuilderService.buildSystemPrompt(mergedContext);
            String aiExplanation = geminiClientService.generateExplanation(mergedPrompt);
            
            mergedExplanation.insert(0, "=== AI-Generated Merged Analysis ===\n");
            mergedExplanation.insert(0, aiExplanation);
            mergedExplanation.insert(0, "\n\n");
        }

        return new AnalysisResponse(
            fileNames,
            allCodeElements.isEmpty() ? null : allCodeElements,
            allDiagramElements.isEmpty() ? null : allDiagramElements,
            mergedContext,
            mergedExplanation.toString()
        );
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new HealthResponse("System Analysis API is running", 
            cacheService.getCacheSize()));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<?> clearCache() {
        cacheService.clearAllCache();
        return ResponseEntity.ok("Cache cleared successfully");
    }

    // Response DTOs
    public static class AnalysisResult {
        private final String fileName;
        private final InputType inputType;
        private final List<CodeElement> codeElements;
        private final List<DiagramElement> diagramElements;
        private final SystemContext systemContext;
        private final String explanation;
        private final String systemId;

        public AnalysisResult(String fileName, InputType inputType, List<CodeElement> codeElements,
                             List<DiagramElement> diagramElements, SystemContext systemContext,
                             String explanation, String systemId) {
            this.fileName = fileName;
            this.inputType = inputType;
            this.codeElements = codeElements;
            this.diagramElements = diagramElements;
            this.systemContext = systemContext;
            this.explanation = explanation;
            this.systemId = systemId;
        }

        // Getters
        public String getFileName() { return fileName; }
        public InputType getInputType() { return inputType; }
        public List<CodeElement> getCodeElements() { return codeElements; }
        public List<DiagramElement> getDiagramElements() { return diagramElements; }
        public SystemContext getSystemContext() { return systemContext; }
        public String getExplanation() { return explanation; }
        public String getSystemId() { return systemId; }
    }

    public static class AnalysisResponse {
        private final List<String> fileNames;
        private final List<CodeElement> codeElements;
        private final List<DiagramElement> diagramElements;
        private final SystemContext systemContext;
        private final String explanation;

        public AnalysisResponse(List<String> fileNames, List<CodeElement> codeElements,
                               List<DiagramElement> diagramElements, SystemContext systemContext,
                               String explanation) {
            this.fileNames = fileNames;
            this.codeElements = codeElements;
            this.diagramElements = diagramElements;
            this.systemContext = systemContext;
            this.explanation = explanation;
        }

        // Getters
        public List<String> getFileNames() { return fileNames; }
        public List<CodeElement> getCodeElements() { return codeElements; }
        public List<DiagramElement> getDiagramElements() { return diagramElements; }
        public SystemContext getSystemContext() { return systemContext; }
        public String getExplanation() { return explanation; }
    }

    public static class HealthResponse {
        private final String status;
        private final long cacheSize;

        public HealthResponse(String status, long cacheSize) {
            this.status = status;
            this.cacheSize = cacheSize;
        }

        public String getStatus() { return status; }
        public long getCacheSize() { return cacheSize; }
    }
}
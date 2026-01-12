package codeovai.codeovai.service.analyze;

import codeovai.codeovai.model.*;
import codeovai.codeovai.service.core.CacheService;
import codeovai.codeovai.service.core.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedSystemContextBuilderService {

    private final LoggingService loggingService;
    private final CacheService cacheService;
    
    public SystemContext buildContextFromCode(List<CodeElement> codeElements) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("CONTEXT_BUILDING", "Building context from code elements", 
            "elements", String.valueOf(codeElements.size()));
        
        try {
            // Check cache first
            String contextHash = generateContextHash(codeElements);
            SystemContext cached = cacheService.getCachedAnalysisResult(contextHash, "code_context", SystemContext.class);
            if (cached != null) {
                loggingService.logPipelineStep("CONTEXT_BUILDING", "Using cached context");
                return cached;
            }
            
            SystemContext context = new SystemContext();
            
            // Build system purpose
            context.setSystemPurpose(determineSystemPurpose(codeElements));
            
            // Build architecture overview
            context.setArchitectureOverview(determineArchitecture(codeElements));
            
            // Build core flow
            context.setCoreFlow(determineCoreFlow(codeElements));
            
            // Identify assumptions
            context.setAssumptions(determineAssumptions(codeElements));
            
            // Identify unknowns
            context.setUnknowns(determineUnknowns(codeElements));
            
            // Cache the result
            cacheService.cacheAnalysisResult(contextHash, "code_context", context);
            
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("CONTEXT_BUILDING", "complete_context_building", duration);
            
            return context;
        } catch (Exception e) {
            loggingService.logError("CONTEXT_BUILDING", "Failed to build context from code", e);
            throw new RuntimeException("Context building failed", e);
        }
    }
    
    public SystemContext buildContextFromDiagram(List<DiagramElement> diagramElements) {
        long startTime = System.currentTimeMillis();
        loggingService.logPipelineStep("CONTEXT_BUILDING", "Building context from diagram elements", 
            "elements", String.valueOf(diagramElements.size()));
        
        try {
            // Check cache first
            String contextHash = generateDiagramContextHash(diagramElements);
            SystemContext cached = cacheService.getCachedAnalysisResult(contextHash, "diagram_context", SystemContext.class);
            if (cached != null) {
                loggingService.logPipelineStep("CONTEXT_BUILDING", "Using cached diagram context");
                return cached;
            }
            
            SystemContext context = new SystemContext();
            
            // Build system purpose
            context.setSystemPurpose(determineSystemPurposeFromDiagram(diagramElements));
            
            // Build architecture overview
            context.setArchitectureOverview(determineArchitectureFromDiagram(diagramElements));
            
            // Build core flow
            context.setCoreFlow(determineCoreFlowFromDiagram(diagramElements));
            
            // Identify assumptions
            context.setAssumptions(determineAssumptionsFromDiagram(diagramElements));
            
            // Identify unknowns
            context.setUnknowns(determineUnknownsFromDiagram(diagramElements));
            
            // Cache the result
            cacheService.cacheAnalysisResult(contextHash, "diagram_context", context);
            
            long duration = System.currentTimeMillis() - startTime;
            loggingService.logPerformance("CONTEXT_BUILDING", "complete_diagram_context_building", duration);
            
            return context;
        } catch (Exception e) {
            loggingService.logError("CONTEXT_BUILDING", "Failed to build context from diagram", e);
            throw new RuntimeException("Diagram context building failed", e);
        }
    }
    
    private String determineSystemPurpose(List<CodeElement> codeElements) {
        List<String> controllers = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("controller")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        List<String> services = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("service")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        List<String> entities = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> 
                           a.toLowerCase().contains("entity") || a.toLowerCase().contains("model")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        if (controllers.isEmpty() && services.isEmpty()) {
            return "Unknown - No recognizable web application components found";
        }
        
        StringBuilder purpose = new StringBuilder();
        
        if (!controllers.isEmpty()) {
            purpose.append("Web application with REST API controllers: ");
            purpose.append(String.join(", ", controllers));
        }
        
        if (!services.isEmpty()) {
            if (purpose.length() > 0) purpose.append(". ");
            purpose.append("Business logic services: ");
            purpose.append(String.join(", ", services));
        }
        
        if (!entities.isEmpty()) {
            if (purpose.length() > 0) purpose.append(". ");
            purpose.append("Data entities: ");
            purpose.append(String.join(", ", entities));
        }
        
        return purpose.toString();
    }
    
    private String determineArchitecture(List<CodeElement> codeElements) {
        Map<String, Long> typeCounts = codeElements.stream()
            .collect(Collectors.groupingBy(CodeElement::getElementType, Collectors.counting()));
        
        StringBuilder architecture = new StringBuilder();
        
        if (typeCounts.containsKey("CLASS")) {
            long classCount = typeCounts.get("CLASS");
            architecture.append("Object-oriented architecture with ").append(classCount).append(" classes");
        }
        
        // Detect common patterns
        boolean hasControllers = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("controller")));
        
        boolean hasServices = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("service")));
        
        boolean hasRepositories = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> 
                            a.toLowerCase().contains("repository") || a.toLowerCase().contains("dao")));
        
        if (hasControllers && hasServices && hasRepositories) {
            if (architecture.length() > 0) architecture.append(". ");
            architecture.append("Three-tier architecture (Controller-Service-Repository pattern)");
        } else if (hasControllers && hasServices) {
            if (architecture.length() > 0) architecture.append(". ");
            architecture.append("Two-tier architecture (Controller-Service pattern)");
        }
        
        // Detect framework
        boolean hasSpringAnnotations = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> 
                            a.toLowerCase().startsWith("org.springframework") ||
                            a.toLowerCase().contains("@restcontroller") ||
                            a.toLowerCase().contains("@service") ||
                            a.toLowerCase().contains("@repository")));
        
        if (hasSpringAnnotations) {
            if (architecture.length() > 0) architecture.append(". ");
            architecture.append("Spring Boot framework detected");
        }
        
        return architecture.length() > 0 ? architecture.toString() : "Unknown architecture pattern";
    }
    
    private CoreFlow determineCoreFlow(List<CodeElement> codeElements) {
        CoreFlow coreFlow = new CoreFlow();
        
        // Identify main components
        List<String> controllers = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("controller")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        List<String> services = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("service")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        List<String> repositories = codeElements.stream()
            .filter(e -> e.getAnnotations() != null && 
                       e.getAnnotations().stream().anyMatch(a -> 
                           a.toLowerCase().contains("repository") || a.toLowerCase().contains("dao")))
            .map(CodeElement::getName)
            .collect(Collectors.toList());
        
        coreFlow.setMainComponents(String.join(", ", controllers));
        
        // Determine data flow
        StringBuilder dataFlow = new StringBuilder();
        if (!controllers.isEmpty()) {
            dataFlow.append("Request enters through controllers: ").append(String.join(", ", controllers));
        }
        if (!services.isEmpty()) {
            if (dataFlow.length() > 0) dataFlow.append(" → ");
            dataFlow.append("Business logic processed by services: ").append(String.join(", ", services));
        }
        if (!repositories.isEmpty()) {
            if (dataFlow.length() > 0) dataFlow.append(" → ");
            dataFlow.append("Data access through repositories: ").append(String.join(", ", repositories));
        }
        coreFlow.setDataFlow(dataFlow.toString());
        
        // Determine control flow
        coreFlow.setControlFlow("Unknown - Requires runtime analysis to determine exact control flow");
        
        return coreFlow;
    }
    
    private List<String> determineAssumptions(List<CodeElement> codeElements) {
        List<String> assumptions = new ArrayList<>();
        
        // Check for database entities
        boolean hasEntities = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> 
                            a.toLowerCase().contains("entity") || a.toLowerCase().contains("model")));
        
        if (hasEntities) {
            assumptions.add("System uses database persistence (entities detected)");
        }
        
        // Check for REST controllers
        boolean hasRestControllers = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("restcontroller")));
        
        if (hasRestControllers) {
            assumptions.add("System provides REST API endpoints");
        }
        
        // Check for dependency injection
        boolean hasDependencyInjection = codeElements.stream()
            .anyMatch(e -> e.getAnnotations() != null && 
                        e.getAnnotations().stream().anyMatch(a -> 
                            a.toLowerCase().contains("@autowired") ||
                            a.toLowerCase().contains("@inject")));
        
        if (hasDependencyInjection) {
            assumptions.add("System uses dependency injection");
        }
        
        return assumptions;
    }
    
    private List<String> determineUnknowns(List<CodeElement> codeElements) {
        List<String> unknowns = new ArrayList<>();
        
        unknowns.add("Database schema and relationships (requires database analysis)");
        unknowns.add("API endpoint details and request/response formats (requires controller method analysis)");
        unknowns.add("Business logic implementation details (requires method body analysis)");
        unknowns.add("Security configuration and authentication mechanisms");
        unknowns.add("Error handling strategies and exception management");
        unknowns.add("Performance characteristics and bottlenecks");
        unknowns.add("External system integrations and dependencies");
        unknowns.add("Configuration and deployment environment details");
        
        return unknowns;
    }
    
    private String determineSystemPurposeFromDiagram(List<DiagramElement> diagramElements) {
        Map<String, Long> typeCounts = diagramElements.stream()
            .collect(Collectors.groupingBy(DiagramElement::getType, Collectors.counting()));
        
        StringBuilder purpose = new StringBuilder();
        
        if (typeCounts.containsKey("CONTROLLER")) {
            purpose.append("Web application system with ");
            purpose.append(typeCounts.get("CONTROLLER")).append(" controller components");
        }
        
        if (typeCounts.containsKey("SERVICE")) {
            if (purpose.length() > 0) purpose.append(". ");
            purpose.append("Business logic layer with ");
            purpose.append(typeCounts.get("SERVICE")).append(" service components");
        }
        
        if (typeCounts.containsKey("ENTITY")) {
            if (purpose.length() > 0) purpose.append(". ");
            purpose.append("Data model with ");
            purpose.append(typeCounts.get("ENTITY")).append(" entity components");
        }
        
        return purpose.length() > 0 ? purpose.toString() : "Unknown system purpose - diagram analysis incomplete";
    }
    
    private String determineArchitectureFromDiagram(List<DiagramElement> diagramElements) {
        Map<String, Long> typeCounts = diagramElements.stream()
            .collect(Collectors.groupingBy(DiagramElement::getType, Collectors.counting()));
        
        StringBuilder architecture = new StringBuilder();
        
        // Detect multi-tier architecture
        boolean hasControllers = typeCounts.containsKey("CONTROLLER");
        boolean hasServices = typeCounts.containsKey("SERVICE");
        boolean hasEntities = typeCounts.containsKey("ENTITY");
        boolean hasRepositories = typeCounts.containsKey("REPOSITORY");
        
        if (hasControllers && hasServices && hasRepositories) {
            architecture.append("Three-tier architecture detected (Presentation-Business-Data layers)");
        } else if (hasControllers && hasServices) {
            architecture.append("Two-tier architecture detected (Presentation-Business layers)");
        } else if (hasServices && hasEntities) {
            architecture.append("Business-Data architecture detected");
        } else {
            architecture.append("Unknown architecture pattern");
        }
        
        // Count relationships to infer complexity
        long relationshipCount = diagramElements.stream()
            .mapToLong(e -> e.getRelationships() != null ? e.getRelationships().size() : 0)
            .sum();
        
        if (relationshipCount > 0) {
            if (architecture.length() > 0) architecture.append(". ");
            architecture.append("System contains ").append(relationshipCount).append(" component relationships");
        }
        
        return architecture.toString();
    }
    
    private CoreFlow determineCoreFlowFromDiagram(List<DiagramElement> diagramElements) {
        CoreFlow coreFlow = new CoreFlow();
        
        // Extract main components from diagram
        List<String> mainComponents = diagramElements.stream()
            .filter(e -> !e.getType().equals("CLASS") || 
                       (e.getStereotypes() != null && 
                        !e.getStereotypes().isEmpty()))
            .map(DiagramElement::getName)
            .collect(Collectors.toList());
        
        coreFlow.setMainComponents(String.join(", ", mainComponents));
        
        // Analyze relationships for data flow
        StringBuilder dataFlow = new StringBuilder();
        for (DiagramElement element : diagramElements) {
            if (element.getRelationships() != null) {
                for (var relationship : element.getRelationships()) {
                    if (dataFlow.length() > 0) dataFlow.append(" → ");
                    dataFlow.append(relationship.getFrom())
                           .append(" to ")
                           .append(relationship.getTo())
                           .append(" (").append(relationship.getType()).append(")");
                }
            }
        }
        
        coreFlow.setDataFlow(dataFlow.length() > 0 ? dataFlow.toString() : "Unknown data flow - relationships not clearly defined");
        coreFlow.setControlFlow("Unknown control flow - requires additional behavioral information");
        
        return coreFlow;
    }
    
    private List<String> determineAssumptionsFromDiagram(List<DiagramElement> diagramElements) {
        List<String> assumptions = new ArrayList<>();
        
        // Check for entity components
        boolean hasEntities = diagramElements.stream()
            .anyMatch(e -> e.getType().equals("ENTITY"));
        
        if (hasEntities) {
            assumptions.add("System manages persistent data entities");
        }
        
        // Check for controller components
        boolean hasControllers = diagramElements.stream()
            .anyMatch(e -> e.getType().equals("CONTROLLER"));
        
        if (hasControllers) {
            assumptions.add("System provides user interface or API endpoints");
        }
        
        // Check for service components
        boolean hasServices = diagramElements.stream()
            .anyMatch(e -> e.getType().equals("SERVICE"));
        
        if (hasServices) {
            assumptions.add("System implements business logic services");
        }
        
        return assumptions;
    }
    
    private List<String> determineUnknownsFromDiagram(List<DiagramElement> diagramElements) {
        List<String> unknowns = new ArrayList<>();
        
        unknowns.add("Implementation details of each component");
        unknowns.add("Data structures and attribute types");
        unknowns.add("Method signatures and business logic");
        unknowns.add("Security mechanisms and access controls");
        unknowns.add("Performance characteristics and scalability");
        unknowns.add("External system integrations");
        unknowns.add("Error handling and exception management");
        unknowns.add("Configuration and deployment details");
        
        return unknowns;
    }
    
    private String generateContextHash(List<CodeElement> codeElements) {
        // Generate a hash based on element names, types, and annotations
        StringBuilder content = new StringBuilder();
        codeElements.stream()
            .sorted(Comparator.comparing(CodeElement::getName))
            .forEach(e -> content.append(e.getName())
                                .append(e.getElementType())
                                .append(e.getAnnotations() != null ? String.join("", e.getAnnotations()) : ""));
        
        return cacheService.generateFileHash(content.toString().getBytes());
    }
    
    private String generateDiagramContextHash(List<DiagramElement> diagramElements) {
        // Generate a hash based on diagram elements and relationships
        StringBuilder content = new StringBuilder();
        diagramElements.stream()
            .sorted(Comparator.comparing(DiagramElement::getName))
            .forEach(e -> content.append(e.getName())
                                .append(e.getType())
                                .append(e.getStereotypes() != null ? String.join("", e.getStereotypes()) : ""));
        
        return cacheService.generateFileHash(content.toString().getBytes());
    }
}

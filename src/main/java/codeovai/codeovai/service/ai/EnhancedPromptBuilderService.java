package codeovai.codeovai.service.ai;

import codeovai.codeovai.model.CodeElement;
import codeovai.codeovai.model.DiagramElement;
import codeovai.codeovai.model.SystemContext;
import codeovai.codeovai.service.core.LoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedPromptBuilderService {

    private final LoggingService loggingService;
    
    public String buildSystemPrompt(SystemContext systemContext) {
        loggingService.logPipelineStep("PROMPT_BUILDING", "Building enhanced system prompt");
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("SYSTEM ANALYSIS REQUEST\n");
        prompt.append("=====================\n\n");
        
        // System Purpose Section
        prompt.append("SYSTEM_PURPOSE:\n");
        if (systemContext.getSystemPurpose() != null && !systemContext.getSystemPurpose().isEmpty()) {
            prompt.append(systemContext.getSystemPurpose()).append("\n\n");
        } else {
            prompt.append("Unknown - System purpose could not be determined from the analysis\n\n");
        }
        
        // Architecture Overview Section
        prompt.append("ARCHITECTURE:\n");
        if (systemContext.getArchitectureOverview() != null && !systemContext.getArchitectureOverview().isEmpty()) {
            prompt.append(systemContext.getArchitectureOverview()).append("\n\n");
        } else {
            prompt.append("Unknown - Architecture details could not be determined\n\n");
        }
        
        // Core Flow Section
        prompt.append("CORE_FLOW:\n");
        if (systemContext.getCoreFlow() != null) {
            prompt.append("Main Components: ").append(systemContext.getCoreFlow().getMainComponents()).append("\n");
            prompt.append("Data Flow: ").append(systemContext.getCoreFlow().getDataFlow()).append("\n");
            prompt.append("Control Flow: ").append(systemContext.getCoreFlow().getControlFlow()).append("\n");
        } else {
            prompt.append("Unknown - Core flow could not be determined\n");
        }
        prompt.append("\n");
        
        // Risk Zones Section
        prompt.append("RISK_ZONES:\n");
        prompt.append("1. Authentication & Authorization: Unknown - requires manual verification\n");
        prompt.append("2. Data Validation: Unknown - requires manual verification\n");
        prompt.append("3. Error Handling: Unknown - requires manual verification\n");
        prompt.append("4. Performance: Unknown - requires manual verification\n");
        prompt.append("5. Security: Unknown - requires manual verification\n\n");
        
        // Safe Change Advice Section
        prompt.append("SAFE_CHANGE_ADVICE:\n");
        prompt.append("1. Always backup the system before making changes\n");
        prompt.append("2. Test changes in a development environment first\n");
        prompt.append("3. Run comprehensive tests after modifications\n");
        prompt.append("4. Monitor system performance after deployment\n");
        prompt.append("5. Document all changes for future reference\n\n");
        
        // Uncertainties Section
        prompt.append("UNCERTAINTIES:\n");
        if (systemContext.getUnknowns() != null && !systemContext.getUnknowns().isEmpty()) {
            for (String unknown : systemContext.getUnknowns()) {
                prompt.append("- ").append(unknown).append("\n");
            }
        } else {
            prompt.append("- No specific uncertainties identified during analysis\n");
        }
        prompt.append("\n");
        
        // Analysis Instructions
        prompt.append("ANALYSIS_INSTRUCTIONS:\n");
        prompt.append("Please provide a comprehensive analysis of this system including:\n");
        prompt.append("1. Overall system functionality and purpose\n");
        prompt.append("2. Architecture patterns and design decisions\n");
        prompt.append("3. Key components and their responsibilities\n");
        prompt.append("4. Data flow and control flow analysis\n");
        prompt.append("5. Potential risks and security considerations\n");
        prompt.append("6. Recommendations for improvements\n");
        prompt.append("7. Areas that require further investigation\n\n");
        
        prompt.append("IMPORTANT: Always explicitly state 'Unknown' when information cannot be determined from the provided context.\n");
        
        return prompt.toString();
    }
    
    public String buildCodeAnalysisPrompt(List<CodeElement> codeElements) {
        loggingService.logPipelineStep("PROMPT_BUILDING", "Building code analysis prompt", 
            "elements", String.valueOf(codeElements.size()));
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("CODE ANALYSIS REQUEST\n");
        prompt.append("====================\n\n");
        
        // Group elements by type
        Map<String, List<CodeElement>> groupedElements = codeElements.stream()
            .collect(Collectors.groupingBy(CodeElement::getElementType));
        
        // Controllers
        if (groupedElements.containsKey("CLASS")) {
            List<CodeElement> controllers = groupedElements.get("CLASS").stream()
                .filter(e -> e.getAnnotations() != null && 
                           e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("controller")))
                .collect(Collectors.toList());
            
            if (!controllers.isEmpty()) {
                prompt.append("CONTROLLERS:\n");
                for (CodeElement controller : controllers) {
                    prompt.append(formatCodeElement(controller));
                }
                prompt.append("\n");
            }
        }
        
        // Services
        if (groupedElements.containsKey("CLASS")) {
            List<CodeElement> services = groupedElements.get("CLASS").stream()
                .filter(e -> e.getAnnotations() != null && 
                           e.getAnnotations().stream().anyMatch(a -> a.toLowerCase().contains("service")))
                .collect(Collectors.toList());
            
            if (!services.isEmpty()) {
                prompt.append("SERVICES:\n");
                for (CodeElement service : services) {
                    prompt.append(formatCodeElement(service));
                }
                prompt.append("\n");
            }
        }
        
        // Models/Entities
        if (groupedElements.containsKey("CLASS")) {
            List<CodeElement> models = groupedElements.get("CLASS").stream()
                .filter(e -> e.getAnnotations() != null && 
                           e.getAnnotations().stream().anyMatch(a -> 
                               a.toLowerCase().contains("entity") || a.toLowerCase().contains("model")))
                .collect(Collectors.toList());
            
            if (!models.isEmpty()) {
                prompt.append("MODELS/ENTITIES:\n");
                for (CodeElement model : models) {
                    prompt.append(formatCodeElement(model));
                }
                prompt.append("\n");
            }
        }
        
        // Repositories
        if (groupedElements.containsKey("CLASS")) {
            List<CodeElement> repositories = groupedElements.get("CLASS").stream()
                .filter(e -> e.getAnnotations() != null && 
                           e.getAnnotations().stream().anyMatch(a -> 
                               a.toLowerCase().contains("repository") || a.toLowerCase().contains("dao")))
                .collect(Collectors.toList());
            
            if (!repositories.isEmpty()) {
                prompt.append("REPOSITORIES:\n");
                for (CodeElement repository : repositories) {
                    prompt.append(formatCodeElement(repository));
                }
                prompt.append("\n");
            }
        }
        
        // Other Classes
        if (groupedElements.containsKey("CLASS")) {
            List<CodeElement> others = groupedElements.get("CLASS").stream()
                .filter(e -> e.getAnnotations() == null || 
                           e.getAnnotations().stream().noneMatch(a -> 
                               a.toLowerCase().contains("controller") || 
                               a.toLowerCase().contains("service") ||
                               a.toLowerCase().contains("entity") ||
                               a.toLowerCase().contains("model") ||
                               a.toLowerCase().contains("repository") ||
                               a.toLowerCase().contains("dao")))
                .collect(Collectors.toList());
            
            if (!others.isEmpty()) {
                prompt.append("OTHER CLASSES:\n");
                for (CodeElement other : others) {
                    prompt.append(formatCodeElement(other));
                }
                prompt.append("\n");
            }
        }
        
        // Methods
        if (groupedElements.containsKey("METHOD")) {
            prompt.append("METHODS:\n");
            for (CodeElement method : groupedElements.get("METHOD")) {
                prompt.append(formatCodeElement(method));
            }
            prompt.append("\n");
        }
        
        // Analysis Instructions
        prompt.append("ANALYSIS_INSTRUCTIONS:\n");
        prompt.append("Analyze the code structure above and provide:\n");
        prompt.append("1. System purpose and main functionality\n");
        prompt.append("2. Architecture pattern identification\n");
        prompt.append("3. Key components and their roles\n");
        prompt.append("4. Data flow between components\n");
        prompt.append("5. API endpoints and their purposes\n");
        prompt.append("6. Database entities and relationships\n");
        prompt.append("7. Potential security concerns\n");
        prompt.append("8. Areas requiring further investigation\n\n");
        
        prompt.append("IMPORTANT: Always explicitly state 'Unknown' when information cannot be determined.\n");
        
        return prompt.toString();
    }
    
    public String buildDiagramAnalysisPrompt(List<DiagramElement> diagramElements) {
        loggingService.logPipelineStep("PROMPT_BUILDING", "Building diagram analysis prompt", 
            "elements", String.valueOf(diagramElements.size()));
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("DIAGRAM ANALYSIS REQUEST\n");
        prompt.append("========================\n\n");
        
        // Group elements by type
        Map<String, List<DiagramElement>> groupedElements = diagramElements.stream()
            .collect(Collectors.groupingBy(DiagramElement::getType));
        
        // Controllers
        if (groupedElements.containsKey("CONTROLLER")) {
            prompt.append("CONTROLLERS:\n");
            for (DiagramElement controller : groupedElements.get("CONTROLLER")) {
                prompt.append(formatDiagramElement(controller));
            }
            prompt.append("\n");
        }
        
        // Services
        if (groupedElements.containsKey("SERVICE")) {
            prompt.append("SERVICES:\n");
            for (DiagramElement service : groupedElements.get("SERVICE")) {
                prompt.append(formatDiagramElement(service));
            }
            prompt.append("\n");
        }
        
        // Entities
        if (groupedElements.containsKey("ENTITY")) {
            prompt.append("ENTITIES:\n");
            for (DiagramElement entity : groupedElements.get("ENTITY")) {
                prompt.append(formatDiagramElement(entity));
            }
            prompt.append("\n");
        }
        
        // Repositories
        if (groupedElements.containsKey("REPOSITORY")) {
            prompt.append("REPOSITORIES:\n");
            for (DiagramElement repository : groupedElements.get("REPOSITORY")) {
                prompt.append(formatDiagramElement(repository));
            }
            prompt.append("\n");
        }
        
        // General Classes
        if (groupedElements.containsKey("CLASS")) {
            prompt.append("CLASSES:\n");
            for (DiagramElement clazz : groupedElements.get("CLASS")) {
                prompt.append(formatDiagramElement(clazz));
            }
            prompt.append("\n");
        }
        
        // Components
        if (groupedElements.containsKey("COMPONENT")) {
            prompt.append("COMPONENTS:\n");
            for (DiagramElement component : groupedElements.get("COMPONENT")) {
                prompt.append(formatDiagramElement(component));
            }
            prompt.append("\n");
        }
        
        // Relationships Summary
        prompt.append("RELATIONSHIPS:\n");
        for (DiagramElement element : diagramElements) {
            if (element.getRelationships() != null && !element.getRelationships().isEmpty()) {
                for (var relationship : element.getRelationships()) {
                    prompt.append(String.format("- %s -> %s (%s) [%s]\n",
                        relationship.getFrom(),
                        relationship.getTo(),
                        relationship.getType(),
                        relationship.getCardinality()));
                }
            }
        }
        prompt.append("\n");
        
        // Analysis Instructions
        prompt.append("ANALYSIS_INSTRUCTIONS:\n");
        prompt.append("Analyze the diagram structure above and provide:\n");
        prompt.append("1. System architecture and design patterns\n");
        prompt.append("2. Component responsibilities and interactions\n");
        prompt.append("3. Data flow and control flow\n");
        prompt.append("4. Relationship types and their implications\n");
        prompt.append("5. System boundaries and interfaces\n");
        prompt.append("6. Potential design issues or improvements\n");
        prompt.append("7. Missing components or relationships\n");
        prompt.append("8. Overall system assessment\n\n");
        
        prompt.append("IMPORTANT: Always explicitly state 'Unknown' when information cannot be determined.\n");
        
        return prompt.toString();
    }
    
    private String formatCodeElement(CodeElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(element.getName()).append(" (").append(element.getElementType()).append(")\n");
        
        if (element.getSourceType() != null) {
            sb.append("  Language: ").append(element.getSourceType()).append("\n");
        }
        
        if (element.getFilePath() != null) {
            sb.append("  File: ").append(element.getFilePath()).append("\n");
        }
        
        if (element.getVisibility() != null) {
            sb.append("  Visibility: ").append(element.getVisibility()).append("\n");
        }
        
        if (element.isAbstract()) {
            sb.append("  Abstract: Yes\n");
        }
        
        if (element.isStatic()) {
            sb.append("  Static: Yes\n");
        }
        
        if (element.getAnnotations() != null && !element.getAnnotations().isEmpty()) {
            sb.append("  Annotations: ").append(String.join(", ", element.getAnnotations())).append("\n");
        }
        
        if (element.getMethods() != null && !element.getMethods().isEmpty()) {
            sb.append("  Methods: ").append(String.join(", ", element.getMethods())).append("\n");
        }
        
        if (element.getReturnType() != null) {
            sb.append("  Return Type: ").append(element.getReturnType()).append("\n");
        }
        
        if (element.getParameters() != null && !element.getParameters().isEmpty()) {
            sb.append("  Parameters: ").append(String.join(", ", element.getParameters())).append("\n");
        }
        
        if (element.getParentClass() != null) {
            sb.append("  Extends: ").append(element.getParentClass()).append("\n");
        }
        
        if (element.getInterfaces() != null && !element.getInterfaces().isEmpty()) {
            sb.append("  Implements: ").append(String.join(", ", element.getInterfaces())).append("\n");
        }
        
        return sb.toString();
    }
    
    private String formatDiagramElement(DiagramElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(element.getName()).append(" (").append(element.getType()).append(")\n");
        
        if (element.getStereotypes() != null && !element.getStereotypes().isEmpty()) {
            sb.append("  Stereotypes: ").append(String.join(", ", element.getStereotypes())).append("\n");
        }
        
        if (element.getVisibility() != null) {
            sb.append("  Visibility: ").append(element.getVisibility()).append("\n");
        }
        
        if (element.isAbstract()) {
            sb.append("  Abstract: Yes\n");
        }
        
        if (element.getAttributes() != null && !element.getAttributes().isEmpty()) {
            sb.append("  Attributes: ").append(String.join(", ", element.getAttributes())).append("\n");
        }
        
        if (element.getMethods() != null && !element.getMethods().isEmpty()) {
            sb.append("  Methods: ").append(String.join(", ", element.getMethods())).append("\n");
        }
        
        if (element.getRelationships() != null && !element.getRelationships().isEmpty()) {
            sb.append("  Relationships:\n");
            for (var relationship : element.getRelationships()) {
                sb.append(String.format("    -> %s (%s) [%s]\n",
                    relationship.getTo(),
                    relationship.getType(),
                    relationship.getCardinality()));
            }
        }
        
        return sb.toString();
    }
}

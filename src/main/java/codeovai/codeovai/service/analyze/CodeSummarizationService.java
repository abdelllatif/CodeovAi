package codeovai.codeovai.service.analyze;

import codeovai.codeovai.model.CodeElement;
import codeovai.codeovai.model.CodeSummary;
import codeovai.codeovai.model.SourceFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeSummarizationService {
    List<CodeElement> methods = new ArrayList<>();
    List<CodeElement> endpoints = new ArrayList<>();
    private static final Pattern CLASS_PATTERN =
            Pattern.compile("\\bclass\\s+(\\w+)");

    private static final Pattern METHOD_PATTERN =
            Pattern.compile("(public|private|protected)?\\s*[\\w<>\\[\\]]+\\s+(\\w+)\\s*\\(");

    private static final Pattern ENDPOINT_PATTERN =
            Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\\s*\\(.*?\"(.*?)\"");

    public CodeSummary summarize(List<SourceFile> files, Path systemRoot) {
        List<CodeElement> controllers = new ArrayList<>();
        List<CodeElement> services = new ArrayList<>();
        List<CodeElement> models = new ArrayList<>();
        List<CodeElement> endpoints = new ArrayList<>();

        for (SourceFile file : files) {
            Path filePath = systemRoot.resolve(file.getPath());

            try {
                String content = Files.readString(filePath);

                extractClasses(content, file, controllers, services, models);
                extractMethods(content, file, controllers, services);
                extractEndpoints(content, endpoints);

            } catch (IOException e) {

            }
        }

        return new CodeSummary(controllers, services, models, endpoints);
    }

    private void extractClasses(
            String content,
            SourceFile file,
            List<CodeElement> controllers,
            List<CodeElement> services,
            List<CodeElement> models
    ) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        while (matcher.find()) {
            CodeElement element = new CodeElement();
            element.setName(matcher.group(1));
            element.setElementType("CLASS");
            element.setSourceType(file.getType());


            switch (file.getType()) {
                case "CONTROLLER" -> controllers.add(element);
                case "SERVICE" -> services.add(element);
                case "MODEL" -> models.add(element);
            }
        }
    }

    private void extractMethods(
            String content,
            SourceFile file,
            List<CodeElement> controllers,
            List<CodeElement> services
    ) {
        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            CodeElement element = new CodeElement();
            element.setName(matcher.group(2));
            element.setElementType("METHOD");
            element.setSourceType(file.getType());
            methods.add(element);

            if ("CONTROLLER".equals(file.getType())) {
                controllers.add(element);
            } else if ("SERVICE".equals(file.getType())) {
                services.add(element);
            }
        }
    }

    private void extractEndpoints(String content, List<CodeElement> endpoints) {
        Matcher matcher = ENDPOINT_PATTERN.matcher(content);
        while (matcher.find()) {
            CodeElement endpoint = new CodeElement();
            endpoint.setName(matcher.group(2));
            endpoint.setElementType("ENDPOINT");
            endpoint.setSourceType(matcher.group(1));
            endpoints.add(endpoint);
        }
    }
}

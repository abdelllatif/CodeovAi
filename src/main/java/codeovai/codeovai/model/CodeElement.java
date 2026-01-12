package codeovai.codeovai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeElement {

    private String name;
    private String elementType;
    private String sourceType;
    private String filePath;
    private List<String> methods;
    private List<String> annotations;
    private List<String> dependencies;
    private String visibility;
    private boolean isStatic;
    private boolean isAbstract;
    private String parentClass;
    private List<String> interfaces;
    private String returnType;
    private List<String> parameters;
}

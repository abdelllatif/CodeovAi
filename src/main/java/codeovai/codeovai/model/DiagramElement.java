package codeovai.codeovai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramElement {

    private String name;
    private String type; // CLASS, INTERFACE, ENTITY, COMPONENT, etc.
    private List<String> attributes;
    private List<String> methods;
    private List<String> stereotypes;
    private String visibility;
    private boolean isAbstract;
    private List<DiagramRelationship> relationships;
    private String position; // Optional position information
}

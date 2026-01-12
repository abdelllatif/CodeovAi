package codeovai.codeovai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramRelationship {

    private String from;
    private String to;
    private String type; // ASSOCIATION, INHERITANCE, COMPOSITION, AGGREGATION, DEPENDENCY
    private String cardinality; // 1, *, 0..1, 1..*, etc.
    private String label;
    private boolean isBidirectional;
}

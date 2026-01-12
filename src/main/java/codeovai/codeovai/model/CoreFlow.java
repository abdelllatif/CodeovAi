package codeovai.codeovai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor

@Data
public class CoreFlow {

    private String name;
    private List<String> steps;
    private String entryPoint;
    private String mainComponents;
    private String dataFlow;
    private String controlFlow;
}

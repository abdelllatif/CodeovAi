package codeovai.codeovai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor

@Data
public class SystemContext {

    private String systemPurpose;
    private String architectureOverview;
    private CoreFlow coreFlow;
    private List<String> assumptions;
    private List<String> unknowns;
}

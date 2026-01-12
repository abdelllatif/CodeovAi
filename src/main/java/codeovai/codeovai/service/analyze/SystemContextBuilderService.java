package codeovai.codeovai.service.analyze;

import codeovai.codeovai.model.CodeElement;
import codeovai.codeovai.model.CodeSummary;
import codeovai.codeovai.model.CoreFlow;
import codeovai.codeovai.model.SystemContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemContextBuilderService {

    public SystemContext buildContext(CodeSummary summary) {

        String inferredPurpose = inferSystemPurpose(summary);
        String architectureOverview = inferArchitecture(summary);
        CoreFlow coreFlow = inferCoreFlow(summary);

        List<String> assumptions = buildAssumptions(summary);
        List<String> unknowns = buildUnknowns(summary);

        return new SystemContext(
                inferredPurpose,
                architectureOverview,
                coreFlow,
                assumptions,
                unknowns
        );
    }

    private String inferSystemPurpose(CodeSummary summary) {
        if (!summary.getEndpoints().isEmpty()) {
            return "Backend API providing business operations via REST endpoints.";
        }
        return "Backend system with unclear external interfaces.";
    }

    private String inferArchitecture(CodeSummary summary) {
        return """
                Layered architecture inferred:
                - Controllers handle incoming requests
                - Services contain business logic
                - Models represent domain data
                """;
    }

    private CoreFlow inferCoreFlow(CodeSummary summary) {
        CoreFlow flow = new CoreFlow(); // constructor بلا معطيات

        if (summary.getEndpoints().isEmpty()) {
            flow.setName("Unknown Flow");
            flow.setSteps(List.of("No REST endpoints detected"));
            flow.setEntryPoint("N/A");
            flow.setMainComponents("N/A");
            flow.setDataFlow("N/A");
            flow.setControlFlow("N/A");
            return flow;
        }

        CodeElement entryPoint = summary.getEndpoints().get(0);

        List<String> steps = new ArrayList<>();
        steps.add("Client sends HTTP request to endpoint: " + entryPoint.getName());
        steps.add("Controller receives and validates request");
        steps.add("Service layer processes business logic");
        steps.add("Response is returned to the client");

        flow.setName("Primary API Flow");
        flow.setSteps(steps);
        flow.setEntryPoint(entryPoint.getName());
        flow.setMainComponents("Controller, Service"); // ممكن تغيّر حسب المشروع
        flow.setDataFlow("Request -> Controller -> Service -> Response");
        flow.setControlFlow("Synchronous");

        return flow;
    }


    private List<String> buildAssumptions(CodeSummary summary) {
        List<String> assumptions = new ArrayList<>();
        assumptions.add("Controllers delegate logic to services");
        assumptions.add("Services are stateless");
        assumptions.add("Models represent domain entities");
        return assumptions;
    }

    private List<String> buildUnknowns(CodeSummary summary) {
        List<String> unknowns = new ArrayList<>();

        if (summary.getEndpoints().isEmpty()) {
            unknowns.add("No clear entry points detected");
        }

        if (summary.getServices().isEmpty()) {
            unknowns.add("Service layer responsibilities unclear");
        }

        return unknowns;
    }
}

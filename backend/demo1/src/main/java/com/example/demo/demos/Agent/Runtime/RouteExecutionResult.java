package com.example.demo.demos.Agent.Runtime;

import com.example.demo.demos.Agent.Service.BackendApiProxyService;
import lombok.Data;

@Data
public class RouteExecutionResult {

    private boolean matched;
    private boolean productInternal;
    private boolean success;
    private boolean requiresClarification;
    private String clarificationPrompt;
    private String degradeReason;
    private RouteMatchResult matchResult;
    private NormalizedRouteData routeData;
    private BackendApiProxyService.InvocationResult invocationResult;

    public static RouteExecutionResult notMatched(RouteMatchResult matchResult) {
        RouteExecutionResult result = new RouteExecutionResult();
        result.setMatchResult(matchResult);
        return result;
    }

    public static RouteExecutionResult clarification(RouteMatchResult matchResult, String prompt) {
        RouteExecutionResult result = new RouteExecutionResult();
        result.setMatched(matchResult != null && matchResult.getRoute() != null);
        result.setRequiresClarification(true);
        result.setClarificationPrompt(prompt);
        result.setMatchResult(matchResult);
        result.setDegradeReason("route_requires_clarification");
        return result;
    }

    public static RouteExecutionResult productInternal(RouteMatchResult matchResult) {
        RouteExecutionResult result = new RouteExecutionResult();
        result.setMatched(true);
        result.setProductInternal(true);
        result.setSuccess(true);
        result.setMatchResult(matchResult);
        return result;
    }

    public static RouteExecutionResult success(RouteMatchResult matchResult,
                                               NormalizedRouteData routeData,
                                               BackendApiProxyService.InvocationResult invocationResult) {
        RouteExecutionResult result = new RouteExecutionResult();
        result.setMatched(true);
        result.setSuccess(true);
        result.setMatchResult(matchResult);
        result.setRouteData(routeData);
        result.setInvocationResult(invocationResult);
        return result;
    }

    public static RouteExecutionResult failed(RouteMatchResult matchResult,
                                              String degradeReason,
                                              BackendApiProxyService.InvocationResult invocationResult) {
        RouteExecutionResult result = new RouteExecutionResult();
        result.setMatched(matchResult != null && matchResult.getRoute() != null);
        result.setMatchResult(matchResult);
        result.setDegradeReason(degradeReason);
        result.setInvocationResult(invocationResult);
        return result;
    }
}

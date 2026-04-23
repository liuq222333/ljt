package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentRouterPythonProperties;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.common.schema.ToolPlan;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PythonToolRouterClient {

    private final AgentRouterPythonProperties properties;
    private final PythonSidecarHttpClient httpClient;
    private final PythonSidecarMapper mapper;

    public PythonToolRouterClient(AgentRouterPythonProperties properties,
                                  PythonSidecarHttpClient httpClient,
                                  PythonSidecarMapper mapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public ToolPlan route(ParsedIntent parsedIntent,
                          String keywords,
                          String entityType,
                          boolean executionReady,
                          List<String> missingRequiredSlots,
                          List<String> validationErrors,
                          SessionState.SessionContext sessionContext) {
        if (!properties.isEnabled()) {
            throw new PythonSidecarException("Python route_tools sidecar is disabled");
        }
        PythonSidecarModels.ToolRouterRequestPayload request = mapper.toToolRouterRequest(
                parsedIntent,
                keywords,
                entityType,
                executionReady,
                missingRequiredSlots == null ? Collections.<String>emptyList() : missingRequiredSlots,
                validationErrors == null ? Collections.<String>emptyList() : validationErrors,
                sessionContext
        );
        PythonSidecarModels.ToolPlanPayload response = httpClient.post(
                properties.getBaseUrl(),
                properties.getRoutePath(),
                request,
                PythonSidecarModels.ToolPlanPayload.class,
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs()
        );
        return mapper.toToolPlan(response);
    }
}

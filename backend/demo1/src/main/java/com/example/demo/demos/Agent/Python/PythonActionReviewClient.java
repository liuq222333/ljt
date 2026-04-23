package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentActionReviewPythonProperties;
import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.ActionConversationStore;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PythonActionReviewClient {

    private final AgentActionReviewPythonProperties properties;
    private final PythonSidecarHttpClient httpClient;
    private final PythonSidecarMapper mapper;

    public PythonActionReviewClient(AgentActionReviewPythonProperties properties,
                                    PythonSidecarHttpClient httpClient,
                                    PythonSidecarMapper mapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public PythonSidecarModels.ActionReviewResponsePayload review(String currentMessage,
                                                                  ParsedIntent parsedIntent,
                                                                  ActionConversationStore.PendingAction pendingAction,
                                                                  List<ApiRoute> availableRoutes) {
        if (!properties.isEnabled()) {
            throw new PythonSidecarException("Python review_action sidecar is disabled");
        }
        PythonSidecarModels.ActionReviewRequestPayload request = mapper.toActionReviewRequest(
                currentMessage,
                parsedIntent,
                pendingAction,
                availableRoutes == null ? Collections.<ApiRoute>emptyList() : availableRoutes
        );
        return httpClient.post(
                properties.getBaseUrl(),
                properties.getReviewPath(),
                request,
                PythonSidecarModels.ActionReviewResponsePayload.class,
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs()
        );
    }
}

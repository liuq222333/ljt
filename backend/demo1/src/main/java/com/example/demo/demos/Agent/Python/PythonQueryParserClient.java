package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.QueryParserPythonProperties;
import com.example.demo.demos.Agent.Pojo.AgentChatMessage;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.SessionState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PythonQueryParserClient {

    private final QueryParserPythonProperties properties;
    private final PythonSidecarHttpClient httpClient;
    private final PythonSidecarMapper mapper;

    public PythonQueryParserClient(QueryParserPythonProperties properties,
                                   PythonSidecarHttpClient httpClient,
                                   PythonSidecarMapper mapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public ParsedIntent parse(String currentMessage, List<AgentChatMessage> recentMessages) {
        return parse(currentMessage, recentMessages, null, null);
    }

    public ParsedIntent parse(String currentMessage,
                              List<AgentChatMessage> recentMessages,
                              SessionState.SessionContext sessionContext,
                              Map<String, Object> userProfile) {
        if (!properties.isEnabled()) {
            throw new PythonSidecarException("Python parse_intent sidecar is disabled");
        }
        PythonSidecarModels.QueryParserRequestPayload request =
                mapper.toQueryParserRequest(currentMessage, recentMessages, sessionContext, userProfile);
        PythonSidecarModels.ParsedIntentPayload response = httpClient.post(
                properties.getBaseUrl(),
                properties.getParsePath(),
                request,
                PythonSidecarModels.ParsedIntentPayload.class,
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs()
        );
        return mapper.toParsedIntent(response, currentMessage);
    }
}

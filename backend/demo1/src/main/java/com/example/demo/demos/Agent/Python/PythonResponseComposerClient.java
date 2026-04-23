package com.example.demo.demos.Agent.Python;

import com.example.demo.demos.Agent.Config.AgentComposerPythonProperties;
import com.example.demo.demos.Agent.Pojo.KnowledgeRetrievalResponse;
import com.example.demo.demos.Agent.Pojo.ParsedIntent;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.common.schema.FinalAnswer;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PythonResponseComposerClient {

    private final AgentComposerPythonProperties properties;
    private final PythonSidecarHttpClient httpClient;
    private final PythonSidecarMapper mapper;

    public PythonResponseComposerClient(AgentComposerPythonProperties properties,
                                        PythonSidecarHttpClient httpClient,
                                        PythonSidecarMapper mapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    public FinalAnswer compose(ParsedIntent parsedIntent,
                               String keywords,
                               String entityType,
                               List<ProductSearchSnapshot> searchResults,
                               long searchTotal,
                               KnowledgeRetrievalResponse knowledgeResponse,
                               RealtimeQueryResponse realtimeResponse,
                               SessionState.SessionContext sessionContext,
                               SessionState.ExecutionMeta executionMeta,
                               boolean hasError,
                               String errorCode,
                               String errorMessage,
                               String failedNode) {
        if (!properties.isEnabled()) {
            throw new PythonSidecarException("Python compose_response sidecar is disabled");
        }
        PythonSidecarModels.ResponseComposerRequestPayload request = mapper.toResponseComposerRequest(
                parsedIntent,
                keywords,
                entityType,
                searchResults,
                searchTotal,
                knowledgeResponse,
                realtimeResponse,
                sessionContext,
                executionMeta,
                hasError,
                errorCode,
                errorMessage,
                failedNode
        );
        PythonSidecarModels.FinalAnswerPayload response = httpClient.post(
                properties.getBaseUrl(),
                properties.getComposePath(),
                request,
                PythonSidecarModels.FinalAnswerPayload.class,
                properties.getConnectTimeoutMs(),
                properties.getReadTimeoutMs()
        );
        return mapper.toFinalAnswer(response);
    }
}

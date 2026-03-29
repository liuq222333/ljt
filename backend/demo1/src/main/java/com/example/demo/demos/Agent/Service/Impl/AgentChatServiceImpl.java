package com.example.demo.demos.Agent.Service.Impl;

import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.AgentChatResponse;
import com.example.demo.demos.Agent.Runtime.AgentCheckpointStore;
import com.example.demo.demos.Agent.Runtime.AgentRuntime;
import com.example.demo.demos.Agent.Runtime.SessionState;
import com.example.demo.demos.Agent.Service.AgentChatService;
import com.example.demo.demos.governance.replay.GovernanceReplayStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * W04 Agent Runtime facade.
 * 现阶段不再直接做“单次 LLM + 工具回调”，而是把请求交给节点化 Runtime。
 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentRuntime agentRuntime;
    private final AgentCheckpointStore agentCheckpointStore;
    private final GovernanceReplayStore governanceReplayStore;

    public AgentChatServiceImpl(AgentRuntime agentRuntime,
                                AgentCheckpointStore agentCheckpointStore,
                                GovernanceReplayStore governanceReplayStore) {
        this.agentRuntime = agentRuntime;
        this.agentCheckpointStore = agentCheckpointStore;
        this.governanceReplayStore = governanceReplayStore;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request, String authorization) {
        if (request == null || CollectionUtils.isEmpty(request.getMessages())) {
            throw new IllegalArgumentException("Conversation cannot be empty");
        }
        SessionState state = agentRuntime.run(request, authorization);
        governanceReplayStore.record(request, state);
        AgentChatResponse response = new AgentChatResponse();
        response.setReply(state.getFinalAnswer().getAnswerText());
        response.setFinalAnswer(state.getFinalAnswer());
        response.setModel("agent-runtime");
        response.setRequestId(state.getExecutionMeta().getRequestId());
        response.setTraceId(state.getExecutionMeta().getTraceId());
        response.setSessionId(state.getExecutionMeta().getSessionId());
        response.setCompletedNodes(state.getExecutionMeta().getCompletedNodes());
        response.setDegraded(state.getExecutionMeta().isDegraded());
        response.setRestoredFromCheckpoint(state.getExecutionMeta().isRestoredFromCheckpoint());
        response.setCheckpointCount(agentCheckpointStore.count(state.getExecutionMeta().getSessionId()));
        return response;
    }
}

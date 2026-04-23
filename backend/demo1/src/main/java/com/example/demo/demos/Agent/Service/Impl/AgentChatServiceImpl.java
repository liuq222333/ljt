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
//        创建AgentChatResponse响应对象实例。
        AgentChatResponse response = new AgentChatResponse();
//        设置响应中的回复内容，从最终答案对象中获取答案文本。
        response.setReply(state.getFinalAnswer().getAnswerText());
//        设置响应中的最终答案对象。
        response.setFinalAnswer(state.getFinalAnswer());
//        设置响应使用的模型名称，这里硬编码为"agent-runtime"。
        response.setModel("agent-runtime");
//        从执行元数据中获取请求ID并设置到响应中。
        response.setRequestId(state.getExecutionMeta().getRequestId());
//        从执行元数据中获取追踪ID并设置到响应中，用于链路追踪。
        response.setTraceId(state.getExecutionMeta().getTraceId());
//        从执行元数据中获取会话ID并设置到响应中。
        response.setSessionId(state.getExecutionMeta().getSessionId());
//        设置已完成的节点列表，记录执行过程中完成了哪些处理节点。
        response.setCompletedNodes(state.getExecutionMeta().getCompletedNodes());
//        设置降级状态标志，表示执行是否发生了降级处理。
        response.setDegraded(state.getExecutionMeta().isDegraded());
//        设置是否从检查点恢复的标志，表示执行是否从之前保存的状态恢复。
        response.setRestoredFromCheckpoint(state.getExecutionMeta().isRestoredFromCheckpoint());
//        从检查点存储中查询当前会话的检查点数量并设置到响应中。
        response.setCheckpointCount(agentCheckpointStore.count(state.getExecutionMeta().getSessionId()));
        return response;
    }
}

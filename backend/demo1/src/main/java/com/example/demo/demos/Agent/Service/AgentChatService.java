package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Pojo.AgentChatRequest;
import com.example.demo.demos.Agent.Pojo.AgentChatResponse;
import org.springframework.stereotype.Service;

/**
 * 社区助手的高层服务边界。
 * 具体实现需要负责：调用大模型服务、在需要时执行工具（接口调用），
 * 并返回结构化的统一回复对象。
 */
@Service
public interface AgentChatService {
    /**
     * 处理一次对话轮次并返回助手的回复。
     * @param request 完整的历史消息与可选覆盖参数
     * @param authorization 可选的鉴权令牌，用于安全的工具调用
     */
    AgentChatResponse chat(AgentChatRequest request, String authorization);
}
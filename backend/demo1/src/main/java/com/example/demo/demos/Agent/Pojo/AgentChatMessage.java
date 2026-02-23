package com.example.demo.demos.Agent.Pojo;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * DeepSeek 对话消息的最小单元（角色 + 文本）。
 * DeepSeek 需要完整的历史消息，因此提供 normalize 助手，
 * 以在发送前补齐缺失的角色或空内容。
 */
@Data
public class AgentChatMessage {
    private String role;
    private String content;

    /**
     * 返回一个标准化副本：保证包含角色，且内容非空（至少为空串）。
     */
    public AgentChatMessage normalize() {
        AgentChatMessage message = new AgentChatMessage();
        message.setRole(StringUtils.hasText(role) ? role : "user");
        message.setContent(content == null ? "" : content);
        return message;
    }
}
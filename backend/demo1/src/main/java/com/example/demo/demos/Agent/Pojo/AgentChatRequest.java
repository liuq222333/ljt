package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 前端调用智能助手接口时的请求载体。
 * 仅 {@code messages} 为必填；其他字段用于覆盖
 * {@link com.example.demo.demos.Agent.Config.AgentAiProperties} 中的默认配置。
 */
@Data
public class AgentChatRequest {
    /** 有序的历史消息，新消息放在列表末尾。 */
    private List<AgentChatMessage> messages = Collections.emptyList();
    /** 可选：覆盖模型随机性（temperature）。 */
    private Double temperature;
    /** 可选：覆盖最大生成 Token 数。 */
    private Integer maxTokens;
    /** 可选：是否启用流式回复。 */
    private Boolean stream;
    /** 可选：指定模型名称。 */
    private String model;
}
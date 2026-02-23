package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

import java.util.Map;

/**
 * 智能助手完成一次对话后返回给前端的统一响应。
 * 除文本回复外，还包含模型信息、请求 ID 与用量统计等，便于前端展示与排查。
 */
@Data
public class AgentChatResponse {
    /** 文本回复内容，直接展示给用户。 */
    private String reply;
    /** 实际使用的模型标识。 */
    private String model;
    /** DeepSeek 请求 ID，便于链路追踪与调试。 */
    private String requestId;
    /** Token 用量或其他统计信息，由 DeepSeek 返回。 */
    private Map<String, Object> usage;
}
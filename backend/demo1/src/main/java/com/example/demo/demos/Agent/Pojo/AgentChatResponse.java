package com.example.demo.demos.Agent.Pojo;

import com.example.demo.demos.common.schema.FinalAnswer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
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
    /** Trace ID。 */
    private String traceId;
    /** 运行时 session_id。 */
    private String sessionId;
    /** Token 用量或其他统计信息，由 DeepSeek 返回。 */
    private Map<String, Object> usage;
    /** 结构化最终回答。 */
    private FinalAnswer finalAnswer;
    /** 本次已完成的节点。 */
    private List<String> completedNodes = new ArrayList<>();
    /** 是否发生降级。 */
    private boolean degraded;
    /** 是否从最近 checkpoint 恢复。 */
    private boolean restoredFromCheckpoint;
    /** 当前累计 checkpoint 数量。 */
    private Integer checkpointCount;
}

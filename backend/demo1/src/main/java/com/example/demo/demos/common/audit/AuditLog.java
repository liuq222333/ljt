package com.example.demo.demos.common.audit;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体 — 记录每次请求的完整轨迹。
 * 保留期限不少于 90 天，高风险场景不少于 180 天。
 * 与施工单 W01 审计日志 + 总设计文档 §15.4 对齐。
 */
@Data
public class AuditLog {

    private Long id;

    /** 请求 ID */
    private String requestId;

    /** 追踪 ID */
    private String traceId;

    /** 用户 ID */
    private String userId;

    /** 请求时间 */
    private LocalDateTime timestamp;

    /** 用户原始输入 */
    private String rawInput;

    /** 意图识别结果（JSON） */
    private String parsedIntent;

    /** 工具调用记录（JSON: 调用了哪些工具、参数、耗时、结果摘要） */
    private String toolCalls;

    /** 最终回答摘要 */
    private String finalAnswer;

    /** 是否发生降级 */
    private Boolean isDegraded;

    /** 风险等级（normal / high） */
    private String riskLevel;

    /** 端到端耗时（毫秒） */
    private Long durationMs;

    /** 消耗的 token 数 */
    private Integer tokensUsed;

    /** 创建时间 */
    private LocalDateTime createdAt;
}

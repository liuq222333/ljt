package com.example.demo.demos.common.schema;

import com.example.demo.demos.common.enums.AnswerType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 最终回答 — compose_response 节点的输出。
 * 替代原有的简单 reply 字符串，包含结构化的回答内容。
 * 与施工单 W00 FinalAnswer schema 对齐。
 */
@Data
public class FinalAnswer {

    /** 回答类型 */
    private AnswerType answerType;

    /** 回答文本（LLM 生成的自然语言回答） */
    private String answerText;

    /** 实体卡片列表（搜索结果的结构化展示） */
    private List<EntityCard> cards = new ArrayList<>();

    /** 免责声明列表（如"价格以实际为准"） */
    private List<String> disclaimers = new ArrayList<>();

    /** 引用来源列表（如知识文档的出处） */
    private List<Citation> citations = new ArrayList<>();

    /** 下一步建议操作（如"要看更多吗？""需要查看退款规则吗？"） */
    private List<String> nextActions = new ArrayList<>();

    /** 调试追踪信息 */
    private DebugTrace debugTrace;

    // ========== 内嵌结构 ==========

    /**
     * 实体卡片 — 搜索结果中的单个实体展示。
     */
    @Data
    public static class EntityCard {
        /** 实体 ID */
        private String entityId;
        /** 实体类型（product / event / store） */
        private String entityType;
        /** 标题 */
        private String title;
        /** 副标题 / 简介 */
        private String subtitle;
        /** 图片 URL */
        private String imageUrl;
        /** 价格文本（如"¥99.00"） */
        private String priceText;
        /** 标签列表（如"亲子""周末"） */
        private List<String> tags = new ArrayList<>();
        /** 位置文本 */
        private String locationText;
        /** 实时状态文本（如"有票""营业中"） */
        private String realtimeStatusText;
        /** 推荐理由 */
        private String recommendReason;
    }

    /**
     * 引用来源 — 知识层检索结果的出处标注。
     */
    @Data
    public static class Citation {
        /** 知识文档 ID */
        private String docId;
        /** 文档标题 */
        private String docTitle;
        /** 引用的文本片段 */
        private String snippet;
        /** 置信度 */
        private double confidence;
    }

    /**
     * 调试追踪 — 记录本次请求各节点的执行情况。
     */
    @Data
    public static class DebugTrace {
        /** 请求 ID */
        private String requestId;
        /** 追踪 ID */
        private String traceId;
        /** 各节点执行记录 */
        private List<NodeTrace> nodeTraces = new ArrayList<>();
        /** 端到端总耗时（毫秒） */
        private long totalDurationMs;
        /** 是否发生降级 */
        private boolean degraded = false;
        /** 降级原因（仅降级时有值） */
        private String degradeReason;
    }

    /**
     * 节点追踪 — 单个节点的执行记录。
     */
    @Data
    public static class NodeTrace {
        /** 节点名称（如 parse_intent / normalize_params） */
        private String nodeName;
        /** 开始时间戳 */
        private long startTimeMs;
        /** 耗时（毫秒） */
        private long durationMs;
        /** 是否成功 */
        private boolean success = true;
        /** 错误信息（仅失败时有值） */
        private String errorMessage;
        /** 输入摘要（用于调试，不含敏感数据） */
        private String inputSummary;
        /** 输出摘要 */
        private String outputSummary;
    }
}

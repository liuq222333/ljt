package com.example.demo.demos.common.schema;

import com.example.demo.demos.common.enums.PlanType;
import com.example.demo.demos.common.enums.ToolName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行计划 — route_tools 节点的输出。
 * 描述本次请求需要调用哪些工具、顺序、依赖关系和降级方案。
 * 与施工单 W00 ToolPlan schema 对齐。
 */
@Data
public class ToolPlan {

    /** 计划 ID */
    private String planId;

    /** 计划类型（单工具/澄清/搜索+知识/搜索+实时/并行等） */
    private PlanType planType;

    /** 执行模式（serial / parallel / direct） */
    private String executionMode = "serial";

    /** 是否需要澄清 */
    private boolean requiresClarification = false;

    /** 澄清提示词 */
    private String clarificationPrompt;

    /** 执行步骤列表（按执行顺序排列） */
    private List<ToolStep> steps = new ArrayList<>();

    /** 降级计划（主计划失败时的备选方案） */
    private ToolPlan fallbackPlan;

    /** 路由决策原因（可读文本，用于调试） */
    private String routingReason;

    /** 路由置信度，0.0 ~ 1.0 */
    private double planConfidence = 0.0;

    /** 最大并行工具数 */
    private int maxParallelTools = 1;

    // ========== 内嵌结构 ==========

    @Data
    public static class ToolStep {

        /** 步骤 ID（用于 depends_on 引用） */
        private String stepId;

        /** 工具名 */
        private ToolName toolName;

        /** 步骤用途，如 structured_search / knowledge_retrieval / realtime_check */
        private String purpose;

        /** 输入来源，如 normalized_params / search_results */
        private String inputRef;

        /** 依赖的步骤 ID 列表（为空表示可立即执行） */
        private List<String> dependsOn = new ArrayList<>();

        /** 输出写入的 state 字段 */
        private String outputKey;

        /** 是否允许失败后继续后续流程 */
        private boolean optional = false;

        /** 传给工具的参数（JSON 字符串或结构化对象） */
        private Object params;

        /** 本步骤超时时间（毫秒） */
        private int timeoutMs = 800;
    }
}

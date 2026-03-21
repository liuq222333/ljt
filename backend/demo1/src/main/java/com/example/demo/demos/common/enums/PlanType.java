package com.example.demo.demos.common.enums;

/**
 * 路由计划类型 — Tool Router 输出的执行模式。
 * 与施工单 W00 统一口径对齐。
 */
public enum PlanType {

    /** 单工具执行 */
    SINGLE_TOOL("single_tool", "单工具执行"),

    /** 需要澄清（信息不足，无法路由） */
    CLARIFICATION_REQUIRED("clarification_required", "需要澄清"),

    /** 搜索后知识补充（串行） */
    SEARCH_THEN_KNOWLEDGE("search_then_knowledge", "搜索后知识补充"),

    /** 搜索后实时确认（串行） */
    SEARCH_THEN_REALTIME("search_then_realtime", "搜索后实时确认"),

    /** 搜索后并行知识+实时 */
    SEARCH_THEN_PARALLEL("search_then_parallel", "搜索后并行知识+实时"),

    /** 并行执行多工具 */
    PARALLEL("parallel", "并行执行");

    private final String code;
    private final String description;

    PlanType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PlanType fromCode(String code) {
        if (code == null) {
            return SINGLE_TOOL;
        }
        for (PlanType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return SINGLE_TOOL;
    }
}

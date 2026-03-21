package com.example.demo.demos.common.enums;

/**
 * 参数来源 — normalize_params 阶段为每个参数标记其来源。
 * 用于调试追踪和可解释性（"这个参数从哪来的"）。
 * 与施工单 W00 统一口径对齐。
 */
public enum ParamSource {

    /** 用户显式输入（如用户说了"上海"） */
    EXPLICIT("explicit", "用户显式输入"),

    /** 模型推断（如 LLM 从语义推断出类目） */
    INFERRED("inferred", "模型推断"),

    /** 上下文继承（如多轮对话中从上一轮继承的城市） */
    CONTEXT("context", "上下文继承"),

    /** 系统默认（如默认分页大小、默认排序） */
    DEFAULT("default", "系统默认"),

    /** 规则派生（如根据业务规则自动添加的过滤条件） */
    RULE_DERIVED("rule_derived", "规则派生");

    private final String code;
    private final String description;

    ParamSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ParamSource fromCode(String code) {
        if (code == null) {
            return DEFAULT;
        }
        for (ParamSource source : values()) {
            if (source.code.equalsIgnoreCase(code.trim())) {
                return source;
            }
        }
        return DEFAULT;
    }
}

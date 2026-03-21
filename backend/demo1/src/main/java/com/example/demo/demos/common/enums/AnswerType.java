package com.example.demo.demos.common.enums;

/**
 * 回答类型 — Response Composer 输出的回答分类。
 * 与施工单 W00 统一口径对齐。
 */
public enum AnswerType {

    /** 推荐结果（商品/活动/门店搜索命中） */
    RECOMMENDATION("recommendation", "推荐结果"),

    /** FAQ 回答（知识层命中） */
    FAQ_ANSWER("faq_answer", "FAQ回答"),

    /** 实时确认（库存/营业状态/余票等实时查询） */
    REALTIME_CONFIRMATION("realtime_confirmation", "实时确认"),

    /** 澄清引导（意图不明确，需要用户补充信息） */
    CLARIFICATION("clarification", "澄清引导"),

    /** 无结果（所有工具均未命中） */
    NO_RESULT("no_result", "无结果"),

    /** 部分结果（部分工具成功、部分失败或降级） */
    PARTIAL_RESULT("partial_result", "部分结果");

    private final String code;
    private final String description;

    AnswerType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AnswerType fromCode(String code) {
        if (code == null) {
            return NO_RESULT;
        }
        for (AnswerType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return NO_RESULT;
    }
}

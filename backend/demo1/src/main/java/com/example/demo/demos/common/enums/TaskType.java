package com.example.demo.demos.common.enums;

/**
 * 一级意图分类 — Query Parser 输出的核心字段。
 * 取值与 Query Parser 设计文档 §5.1 对齐。
 * 【W00 统一口径】从 Agent.Enums 迁移到 common.enums，全局统一引用。
 */
public enum TaskType {

    /** 商品搜索/推荐 */
    PRODUCT_SEARCH("product_search", "商品搜索/推荐"),

    /** 活动搜索/推荐 */
    EVENT_SEARCH("event_search", "活动搜索/推荐"),

    /** 门店搜索 */
    STORE_SEARCH("store_search", "门店搜索"),

    /** FAQ / 规则问答 */
    FAQ_QUERY("faq_query", "FAQ/规则问答"),

    /** 强实时查询（"现在还有票吗""还营业吗"） */
    REALTIME_QUERY("realtime_query", "强实时查询"),

    /** 搜索 + 知识混合 */
    MIXED_SEARCH_KNOWLEDGE("mixed_search_knowledge", "搜索+知识"),

    /** 搜索 + 实时混合 */
    MIXED_SEARCH_REALTIME("mixed_search_realtime", "搜索+实时"),

    /** 多轮追问 */
    FOLLOW_UP("follow_up", "多轮追问"),

    /** 用户回答澄清 */
    CLARIFICATION_RESPONSE("clarification_response", "回答澄清"),

    /** 闲聊 / 无法识别 */
    CHITCHAT("chitchat", "闲聊/无法识别");

    private final String code;
    private final String description;

    TaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 字符串查找枚举值，找不到返回 CHITCHAT。
     */
    public static TaskType fromCode(String code) {
        if (code == null) {
            return CHITCHAT;
        }
        for (TaskType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return CHITCHAT;
    }
}

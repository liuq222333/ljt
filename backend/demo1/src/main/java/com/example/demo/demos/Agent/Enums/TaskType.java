package com.example.demo.demos.Agent.Enums;

/**
 * @deprecated 已迁移到 {@link com.example.demo.demos.common.enums.TaskType}，请使用新路径。
 * 本文件仅作为过渡兼容保留，后续版本将删除。
 */
@Deprecated
public enum TaskType {

    PRODUCT_SEARCH("product_search", "商品搜索/推荐"),
    EVENT_SEARCH("event_search", "活动搜索/推荐"),
    STORE_SEARCH("store_search", "门店搜索"),
    FAQ_QUERY("faq_query", "FAQ/规则问答"),
    REALTIME_QUERY("realtime_query", "强实时查询"),
    MIXED_SEARCH_KNOWLEDGE("mixed_search_knowledge", "搜索+知识"),
    MIXED_SEARCH_REALTIME("mixed_search_realtime", "搜索+实时"),
    FOLLOW_UP("follow_up", "多轮追问"),
    CLARIFICATION_RESPONSE("clarification_response", "回答澄清"),
    CHITCHAT("chitchat", "闲聊/无法识别");

    private final String code;
    private final String description;

    TaskType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    /** 转换为新枚举 */
    public com.example.demo.demos.common.enums.TaskType toCommon() {
        return com.example.demo.demos.common.enums.TaskType.fromCode(this.code);
    }

    public static TaskType fromCode(String code) {
        if (code == null) return CHITCHAT;
        for (TaskType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) return type;
        }
        return CHITCHAT;
    }
}

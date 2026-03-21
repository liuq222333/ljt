package com.example.demo.demos.common.enums;

/**
 * 多轮追问类型 — 仅当 is_follow_up=true 时有效。
 * 取值与 Query Parser 设计文档 §7.2 follow_up_type 对齐。
 * 【W00 统一口径】从 Agent.Enums 迁移到 common.enums，全局统一引用。
 */
public enum FollowUpType {

    /** 追加条件（"再便宜一点"） */
    ADD_CONSTRAINT("add_constraint", "追加条件"),

    /** 修改条件（"换成北京的"） */
    CHANGE_CONSTRAINT("change_constraint", "修改条件"),

    /** 否定结果（"不要这个""换一个"） */
    NEGATE_RESULT("negate_result", "否定结果"),

    /** 选择实体（"就第一个吧""这个不错"） */
    SELECT_ENTITY("select_entity", "选择实体"),

    /** 追问详情（"这个能退吗""具体在哪里"） */
    ASK_DETAIL("ask_detail", "追问详情"),

    /** 切换话题（"换个问题""我想问另一个"） */
    SWITCH_TOPIC("switch_topic", "切换话题");

    private final String code;
    private final String description;

    FollowUpType(String code, String description) {
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
     * 根据 code 查找枚举值，找不到返回 null。
     */
    public static FollowUpType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FollowUpType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }
}

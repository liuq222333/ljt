package com.example.demo.demos.Agent.Enums;

/**
 * @deprecated 已迁移到 {@link com.example.demo.demos.common.enums.FollowUpType}，请使用新路径。
 * 本文件仅作为过渡兼容保留，后续版本将删除。
 */
@Deprecated
public enum FollowUpType {

    ADD_CONSTRAINT("add_constraint", "追加条件"),
    CHANGE_CONSTRAINT("change_constraint", "修改条件"),
    NEGATE_RESULT("negate_result", "否定结果"),
    SELECT_ENTITY("select_entity", "选择实体"),
    ASK_DETAIL("ask_detail", "追问详情"),
    SWITCH_TOPIC("switch_topic", "切换话题");

    private final String code;
    private final String description;

    FollowUpType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    /** 转换为新枚举 */
    public com.example.demo.demos.common.enums.FollowUpType toCommon() {
        return com.example.demo.demos.common.enums.FollowUpType.fromCode(this.code);
    }

    public static FollowUpType fromCode(String code) {
        if (code == null) return null;
        for (FollowUpType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) return type;
        }
        return null;
    }
}

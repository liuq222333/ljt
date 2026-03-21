package com.example.demo.demos.common.enums;

/**
 * 知识文档状态 — 知识层文档的生命周期状态。
 * 状态流转：DRAFT -> ACTIVE -> DEPRECATED -> ARCHIVED
 * 与知识层设计文档对齐。
 */
public enum KnowledgeStatus {

    /** 草稿（尚未发布，不参与检索） */
    DRAFT("draft", "草稿"),

    /** 生效中（参与检索） */
    ACTIVE("active", "生效中"),

    /** 已废弃（过期或被新版本替代，不参与检索） */
    DEPRECATED("deprecated", "已废弃"),

    /** 已归档（保留用于审计追溯，不参与检索） */
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String description;

    KnowledgeStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static KnowledgeStatus fromCode(String code) {
        if (code == null) {
            return DRAFT;
        }
        for (KnowledgeStatus status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return DRAFT;
    }
}

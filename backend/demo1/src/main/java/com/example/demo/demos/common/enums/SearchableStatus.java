package com.example.demo.demos.common.enums;

/**
 * 可搜索状态 — 快照表中实体的搜索可见性。
 * 在快照构建阶段统一计算，不在 ES 查询侧临时拼装。
 * 与施工单 W00 统一口径对齐。
 */
public enum SearchableStatus {

    /** 可搜索（正常上架、在有效期内） */
    SEARCHABLE("searchable", "可搜索"),

    /** 不可搜索（未上架或被下架） */
    NOT_SEARCHABLE("not_searchable", "不可搜索"),

    /** 已过期（超出有效期） */
    EXPIRED("expired", "已过期"),

    /** 草稿（尚未发布） */
    DRAFT("draft", "草稿"),

    /** 已禁用（管理员手动禁用） */
    DISABLED("disabled", "已禁用");

    private final String code;
    private final String description;

    SearchableStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SearchableStatus fromCode(String code) {
        if (code == null) {
            return NOT_SEARCHABLE;
        }
        for (SearchableStatus status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return NOT_SEARCHABLE;
    }
}

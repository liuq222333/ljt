package com.example.demo.demos.common.enums;

/**
 * 工具名 — Tool Router 路由时使用的工具标识。
 * 与施工单 W00 统一口径对齐。
 */
public enum ToolName {

    /** 结构化搜索（ES 商品/活动/门店检索） */
    STRUCTURED_SEARCH("structured_search", "结构化搜索"),

    /** 知识检索（FAQ/规则/说明向量检索） */
    KNOWLEDGE_RETRIEVAL("knowledge_retrieval", "知识检索"),

    /** 实时查询（库存/营业状态/余票等实时接口） */
    REALTIME_QUERY("realtime_query", "实时查询");

    private final String code;
    private final String description;

    ToolName(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ToolName fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ToolName name : values()) {
            if (name.code.equalsIgnoreCase(code.trim())) {
                return name;
            }
        }
        return null;
    }
}

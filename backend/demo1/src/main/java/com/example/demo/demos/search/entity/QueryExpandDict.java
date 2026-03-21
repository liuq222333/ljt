package com.example.demo.demos.search.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询扩展词典实体 — 对应 query_expand_dict 表。
 */
@Data
public class QueryExpandDict {

    private Long id;
    private String queryTerm;
    /** 扩展词列表JSON，如 ["手办","模型","公仔"] */
    private String expandTermsJson;
    /** 扩展类目JSON，如 [1001,1002] */
    private String expandCategoriesJson;
    /** 扩展标签JSON，如 [2001,2002] */
    private String expandTagsJson;
    private String status;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}

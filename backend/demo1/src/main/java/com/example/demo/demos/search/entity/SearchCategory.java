package com.example.demo.demos.search.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索类目实体 — 对应 category 表（搜索层独立类目树）。
 */
@Data
public class SearchCategory {

    private Long categoryId;
    private Long parentCategoryId;
    private String categoryName;
    private Integer categoryLevel;
    private String categoryPath;
    private Boolean isLeaf;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

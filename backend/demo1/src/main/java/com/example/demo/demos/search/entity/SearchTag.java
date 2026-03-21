package com.example.demo.demos.search.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索标签实体 — 对应 tag 表。
 */
@Data
public class SearchTag {

    private Long tagId;
    private String tagName;
    private String tagType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

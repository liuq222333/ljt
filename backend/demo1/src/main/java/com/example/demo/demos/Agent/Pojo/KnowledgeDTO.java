package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDTO {
    private Long id;
    private String category;
    private String docType;
    private String title;
    private String content;
    private String summary;
    private String keywords;
    private String relatedQuestions;
    private String sourceSystem;
    private String entityType;
    private String entityId;
    private String cityIds;
    private String categoryIds;
    private String tagIds;
    private String version;
    private Integer priority;
    private String owner;
    private String language;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private LocalDateTime publishedAt;
    private Integer status;
}

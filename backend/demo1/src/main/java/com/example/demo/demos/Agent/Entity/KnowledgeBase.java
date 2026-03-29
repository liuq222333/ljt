package com.example.demo.demos.Agent.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;

    @TableField("doc_type")
    private String docType;

    private String title;
    private String content;
    private String summary;
    private String keywords;

    @TableField("related_questions")
    private String relatedQuestions;

    @TableField("source_system")
    private String sourceSystem;

    @TableField("entity_type")
    private String entityType;

    @TableField("entity_id")
    private String entityId;

    @TableField("city_ids")
    private String cityIds;

    @TableField("category_ids")
    private String categoryIds;

    @TableField("tag_ids")
    private String tagIds;

    private String version;
    private Integer priority;
    private String owner;
    private String language;

    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    @TableField("effective_to")
    private LocalDateTime effectiveTo;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("helpful_count")
    private Integer helpfulCount;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

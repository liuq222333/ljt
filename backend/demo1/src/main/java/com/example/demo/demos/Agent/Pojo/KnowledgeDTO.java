package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

@Data
public class KnowledgeDTO {
    private Long id;
    private String category;
    private String title;
    private String content;
    private String keywords;
    private String relatedQuestions;
    private Integer status;
}

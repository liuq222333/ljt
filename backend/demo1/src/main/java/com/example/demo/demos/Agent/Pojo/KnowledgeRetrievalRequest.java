package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeRetrievalRequest {

    private String queryText;
    private String purpose;
    private String taskType;
    private String entityType;
    private List<String> entityIds = new ArrayList<String>();
    private List<Long> cityIds = new ArrayList<Long>();
    private List<Long> categoryIds = new ArrayList<Long>();
    private List<String> docTypes = new ArrayList<String>();
    private boolean needRerank = true;
    private int topK = 5;
    private LocalDateTime timeContext;
}

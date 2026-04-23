package com.example.demo.demos.Agent.Pojo;

import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeRetrievalResponse {

    private boolean filterApplied;
    private boolean rerankApplied;
    private int candidateCount;
    private int hitCount;
    private String queryVersion;
    private List<KnowledgeBase> items = new ArrayList<KnowledgeBase>();
}

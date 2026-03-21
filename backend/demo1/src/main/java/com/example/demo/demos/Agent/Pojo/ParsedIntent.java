package com.example.demo.demos.Agent.Pojo;

import com.example.demo.demos.common.enums.FollowUpType;
import com.example.demo.demos.common.enums.TaskType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 意图识别结果 — Query Parser（parse_intent 节点）的输出。
 * 结构与 Query Parser 设计文档 §7.1 完全对齐。
 */
@Data
public class ParsedIntent {

    /** 一级意图分类 */
    private TaskType taskType = TaskType.CHITCHAT;

    /** 意图识别置信度，0.0 ~ 1.0 */
    private double intentConfidence = 0.0;

    /** 用户原始输入文本（保留原文，不做修改） */
    private String queryText;

    /** 候选槽位：从用户表达中提取的文本级条件 */
    private CandidateSlots candidateSlots = new CandidateSlots();

    /** 是否包含实时需求信号 */
    private boolean needRealtime = false;

    /** 是否需要知识说明 */
    private boolean needExplanation = false;

    /** 是否需要推荐理由 */
    private boolean needRecommendation = false;

    /** 是否为多轮追问 */
    private boolean followUp = false;

    /** 是否为否定/排除 */
    private boolean negation = false;

    /** 多轮追问类型（仅 followUp=true 时有效） */
    private FollowUpType followUpType;

    /** 被用户否定的实体 ID 列表 */
    private List<String> negatedEntities = new ArrayList<>();
}

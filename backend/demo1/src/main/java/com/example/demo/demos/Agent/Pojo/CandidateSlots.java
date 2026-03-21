package com.example.demo.demos.Agent.Pojo;

import lombok.Data;

/**
 * 候选槽位 — Query Parser 从用户自然语言中提取的文本级条件。
 * 只做文本提取，不做 ID 映射（ID 映射属于 normalize_params 的职责）。
 * 字段与 Query Parser 设计文档 §7.2 candidate_slots 对齐。
 */
@Data
public class CandidateSlots {

    /** 核心搜索词，如"玩具" */
    private String keyword;

    /** 类目文本，如"亲子活动" */
    private String categoryText;

    /** 人群标签文本，如"亲子" */
    private String crowdTagText;

    /** 场景标签文本，如"周末" */
    private String sceneTagText;

    /** 城市文本，如"上海" */
    private String cityText;

    /** 区县文本，如"浦东" */
    private String districtText;

    /** 位置表达，如"附近""离我近" */
    private String locationText;

    /** 价格表达，如"200以内""别太贵" */
    private String priceText;

    /** 时间表达，如"明天""这个周末" */
    private String dateText;

    /** 排序偏好，如"便宜的""评分高" */
    private String sortText;

    /** 目标实体类型：product / event / store */
    private String entityType;

    /** 实体指代，如"这个""第一个""刚才那个" */
    private String entityRef;
}

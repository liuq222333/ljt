package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 助手 LLM 直连相关开关与参数。
 *
 * 控制：Chitchat / Fusion 两条 LLM 兜底链路是否启用、温度、超时与可用性回退策略。
 * 与 deepseek.api.* 配置解耦：DeepSeek 凭据/模型仍读取 DeepSeekProperties；
 * 这里只放"何时调 LLM、用什么参数"。
 */
@Component
@ConfigurationProperties(prefix = "agent.llm")
public class AgentLlmProperties {

    /** Chitchat 走 LLM 是否启用；关闭时 LlmChitchatComposer 直接返回 null，由 Runtime 退回模板。 */
    private boolean chitchatEnabled = true;

    /** 商品问答 LLM 融合是否启用；关闭时 LlmFusionComposer 直接返回 null。 */
    private boolean fusionEnabled = true;

    /** LLM 异常/超时时是否回退到模板。默认 true，避免完全瞎掉。 */
    private boolean fallbackToTemplate = true;

    /** Chitchat 温度（推荐 0.7，自然但不夸张）。 */
    private Double chitchatTemperature = 0.7;

    /** Chitchat 最大 token，回复要求 ≤60 字，给到 200 足够。 */
    private Integer chitchatMaxTokens = 200;

    /** Fusion 温度（推荐 0.3，避免编库存/价格）。 */
    private Double fusionTemperature = 0.3;

    /** Fusion 最大 token。 */
    private Integer fusionMaxTokens = 400;

    /** Fusion 单次读超时（ms），允许比默认更长。 */
    private Long fusionReadTimeoutMs = 8000L;

    /** 商品融合 prompt 中最多序列化几条 ProductSearchSnapshot（默认 5）。 */
    private int maxProducts = 5;

    /** 知识层融合 prompt 中最多序列化几条 KnowledgeBase。 */
    private int maxKnowledge = 3;

    /** 实时层融合 prompt 中最多序列化几条 RealtimeResultItem。 */
    private int maxRealtime = 5;

    public boolean isChitchatEnabled() {
        return chitchatEnabled;
    }

    public void setChitchatEnabled(boolean chitchatEnabled) {
        this.chitchatEnabled = chitchatEnabled;
    }

    public boolean isFusionEnabled() {
        return fusionEnabled;
    }

    public void setFusionEnabled(boolean fusionEnabled) {
        this.fusionEnabled = fusionEnabled;
    }

    public boolean isFallbackToTemplate() {
        return fallbackToTemplate;
    }

    public void setFallbackToTemplate(boolean fallbackToTemplate) {
        this.fallbackToTemplate = fallbackToTemplate;
    }

    public Double getChitchatTemperature() {
        return chitchatTemperature;
    }

    public void setChitchatTemperature(Double chitchatTemperature) {
        this.chitchatTemperature = chitchatTemperature;
    }

    public Integer getChitchatMaxTokens() {
        return chitchatMaxTokens;
    }

    public void setChitchatMaxTokens(Integer chitchatMaxTokens) {
        this.chitchatMaxTokens = chitchatMaxTokens;
    }

    public Double getFusionTemperature() {
        return fusionTemperature;
    }

    public void setFusionTemperature(Double fusionTemperature) {
        this.fusionTemperature = fusionTemperature;
    }

    public Integer getFusionMaxTokens() {
        return fusionMaxTokens;
    }

    public void setFusionMaxTokens(Integer fusionMaxTokens) {
        this.fusionMaxTokens = fusionMaxTokens;
    }

    public Long getFusionReadTimeoutMs() {
        return fusionReadTimeoutMs;
    }

    public void setFusionReadTimeoutMs(Long fusionReadTimeoutMs) {
        this.fusionReadTimeoutMs = fusionReadTimeoutMs;
    }

    public int getMaxProducts() {
        return maxProducts;
    }

    public void setMaxProducts(int maxProducts) {
        this.maxProducts = maxProducts;
    }

    public int getMaxKnowledge() {
        return maxKnowledge;
    }

    public void setMaxKnowledge(int maxKnowledge) {
        this.maxKnowledge = maxKnowledge;
    }

    public int getMaxRealtime() {
        return maxRealtime;
    }

    public void setMaxRealtime(int maxRealtime) {
        this.maxRealtime = maxRealtime;
    }
}

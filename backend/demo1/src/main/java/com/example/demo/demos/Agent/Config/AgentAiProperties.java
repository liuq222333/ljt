package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 服务的类型化配置项。
 * 包含 DeepSeek Chat API 和 OpenAI Embedding API 的配置。
 * 显式暴露所有字段，统一默认值，避免在代码中散落”魔法常量”。
 */
@Component
@ConfigurationProperties(prefix = “openai”)
public class AgentAiProperties {

    /** DeepSeek 提供的 API Key；缺失时所有请求都会失败。 */
    private String key;

    /** OpenAI Embedding API Key；用于知识库向量化 */
    private String embeddingKey;

    /** Embedding API 端点地址 */
    private String embeddingUrl = “https://api.openai.com/v1/embeddings”;

    /** Embedding 模型名称 */
    private String embeddingModel = “text-embedding-3-small”;

    /** 向量维度 */
    private Integer embeddingDimension = 1536;

    /** Chat Completions 的 HTTPS 端点地址。 */
    private String url = "https://api.deepseek.com/chat/completions";

    /** 默认模型名；调用方未设置时使用该值。 */
    private String model = "deepseek-chat";

    /** 温度（随机性）参数，数值越高回复越发散。 */
    private Double temperature = 1d;

    /** 每次生成的最大 Token 上限（安全阈值）。 */
    private Integer maxTokens = 4096;

    /** 是否默认开启流式输出。 */
    private Boolean stream = Boolean.FALSE;

    /** 当代理工具调用到本后端时使用的基础地址。 */
    private String internalApiBaseUrl = "http://localhost:8080";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public String getInternalApiBaseUrl() {
        return internalApiBaseUrl;
    }

    public void setInternalApiBaseUrl(String internalApiBaseUrl) {
        this.internalApiBaseUrl = internalApiBaseUrl;
    }

    public String getEmbeddingKey() {
        return embeddingKey;
    }

    public void setEmbeddingKey(String embeddingKey) {
        this.embeddingKey = embeddingKey;
    }

    public String getEmbeddingUrl() {
        return embeddingUrl;
    }

    public void setEmbeddingUrl(String embeddingUrl) {
        this.embeddingUrl = embeddingUrl;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
}

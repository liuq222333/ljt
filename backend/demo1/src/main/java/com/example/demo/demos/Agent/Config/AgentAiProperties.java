package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
public class AgentAiProperties {

    private String key;
    private String embeddingKey;
    private String embeddingUrl = "https://api.openai.com/v1/embeddings";
    private String embeddingModel = "text-embedding-3-small";
    private Integer embeddingDimension = 1536;
    private String url = "https://api.deepseek.com/chat/completions";
    private String model = "deepseek-chat";
    private Double temperature = 1d;
    private Integer maxTokens = 4096;
    private Boolean stream = Boolean.FALSE;
    private String internalApiBaseUrl = "http://localhost:8080";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
}
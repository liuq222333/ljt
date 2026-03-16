package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置
 */
@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekProperties {

    private String key;
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

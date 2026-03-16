package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI Embedding API 配置
 */
@Component
@ConfigurationProperties(prefix = "openai.embedding")
public class OpenAiEmbeddingProperties {

    private String key;
    private String url = "https://api.openai.com/v1/embeddings";
    private String model = "text-embedding-3-small";
    private Integer dimension = 1536;

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

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }
}

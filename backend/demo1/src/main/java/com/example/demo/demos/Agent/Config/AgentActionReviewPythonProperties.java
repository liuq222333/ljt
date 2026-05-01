package com.example.demo.demos.Agent.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.action-review.python")
public class AgentActionReviewPythonProperties {

    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:9001";
    private String reviewPath = "/review_action";
    private long connectTimeoutMs = 200L;
    private long readTimeoutMs = 900L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getReviewPath() {
        return reviewPath;
    }

    public void setReviewPath(String reviewPath) {
        this.reviewPath = reviewPath;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}

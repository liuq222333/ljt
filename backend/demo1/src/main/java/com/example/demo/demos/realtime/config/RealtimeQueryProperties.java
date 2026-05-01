package com.example.demo.demos.realtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent.realtime")
public class RealtimeQueryProperties {

    private boolean enabled = true;
    private int maxCandidates = 5;
    private int simulatedLatencyMs = 20;
    private String fallbackSource = "snapshot_fallback";
    private String defaultCurrency = "CNY";
    private boolean gatewayEnabled;
    private String gatewayBaseUrl;
    private String gatewayQueryPath = "/query";
    private int gatewayConnectTimeoutMs = 200;
    private int gatewayReadTimeoutMs = 450;
    private int cacheTtlSeconds = 15;
    private int partialCacheTtlSeconds = 5;
    private int degradedCacheTtlSeconds = 5;
    private int gatewayRetryCount = 1;
    private int circuitFailureThreshold = 3;
    private int circuitOpenSeconds = 30;
    private boolean mockGatewayEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getSimulatedLatencyMs() {
        return simulatedLatencyMs;
    }

    public void setSimulatedLatencyMs(int simulatedLatencyMs) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    public String getFallbackSource() {
        return fallbackSource;
    }

    public void setFallbackSource(String fallbackSource) {
        this.fallbackSource = fallbackSource;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public boolean isGatewayEnabled() {
        return gatewayEnabled;
    }

    public void setGatewayEnabled(boolean gatewayEnabled) {
        this.gatewayEnabled = gatewayEnabled;
    }

    public String getGatewayBaseUrl() {
        return gatewayBaseUrl;
    }

    public void setGatewayBaseUrl(String gatewayBaseUrl) {
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    public String getGatewayQueryPath() {
        return gatewayQueryPath;
    }

    public void setGatewayQueryPath(String gatewayQueryPath) {
        this.gatewayQueryPath = gatewayQueryPath;
    }

    public int getGatewayConnectTimeoutMs() {
        return gatewayConnectTimeoutMs;
    }

    public void setGatewayConnectTimeoutMs(int gatewayConnectTimeoutMs) {
        this.gatewayConnectTimeoutMs = gatewayConnectTimeoutMs;
    }

    public int getGatewayReadTimeoutMs() {
        return gatewayReadTimeoutMs;
    }

    public void setGatewayReadTimeoutMs(int gatewayReadTimeoutMs) {
        this.gatewayReadTimeoutMs = gatewayReadTimeoutMs;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public int getPartialCacheTtlSeconds() {
        return partialCacheTtlSeconds;
    }

    public void setPartialCacheTtlSeconds(int partialCacheTtlSeconds) {
        this.partialCacheTtlSeconds = partialCacheTtlSeconds;
    }

    public int getDegradedCacheTtlSeconds() {
        return degradedCacheTtlSeconds;
    }

    public void setDegradedCacheTtlSeconds(int degradedCacheTtlSeconds) {
        this.degradedCacheTtlSeconds = degradedCacheTtlSeconds;
    }

    public int getGatewayRetryCount() {
        return gatewayRetryCount;
    }

    public void setGatewayRetryCount(int gatewayRetryCount) {
        this.gatewayRetryCount = gatewayRetryCount;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public int getCircuitOpenSeconds() {
        return circuitOpenSeconds;
    }

    public void setCircuitOpenSeconds(int circuitOpenSeconds) {
        this.circuitOpenSeconds = circuitOpenSeconds;
    }

    public boolean isMockGatewayEnabled() {
        return mockGatewayEnabled;
    }

    public void setMockGatewayEnabled(boolean mockGatewayEnabled) {
        this.mockGatewayEnabled = mockGatewayEnabled;
    }
}

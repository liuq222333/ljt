package com.example.demo.demos.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "search.es")
public class SearchEsProperties {

    private boolean enabled;
    private boolean initializeOnStartup;
    private String baseUrl;
    private String username;
    private String password;
    private String versionedIndex;
    private String readAlias;
    private String writeAlias;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 5000;
    private int syncBatchSize = 200;
    private String mappingResource;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInitializeOnStartup() {
        return initializeOnStartup;
    }

    public void setInitializeOnStartup(boolean initializeOnStartup) {
        this.initializeOnStartup = initializeOnStartup;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVersionedIndex() {
        return versionedIndex;
    }

    public void setVersionedIndex(String versionedIndex) {
        this.versionedIndex = versionedIndex;
    }

    public String getReadAlias() {
        return readAlias;
    }

    public void setReadAlias(String readAlias) {
        this.readAlias = readAlias;
    }

    public String getWriteAlias() {
        return writeAlias;
    }

    public void setWriteAlias(String writeAlias) {
        this.writeAlias = writeAlias;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getSyncBatchSize() {
        return syncBatchSize;
    }

    public void setSyncBatchSize(int syncBatchSize) {
        this.syncBatchSize = syncBatchSize;
    }

    public String getMappingResource() {
        return mappingResource;
    }

    public void setMappingResource(String mappingResource) {
        this.mappingResource = mappingResource;
    }
}

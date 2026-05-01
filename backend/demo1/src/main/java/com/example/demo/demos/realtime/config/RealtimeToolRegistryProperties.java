package com.example.demo.demos.realtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
@ConfigurationProperties(prefix = "agent.tool-registry")
public class RealtimeToolRegistryProperties {

    private boolean productRealtimeEnabled = true;
    private boolean eventRealtimeEnabled;
    private boolean storeRealtimeEnabled;

    public boolean isRealtimeEnabled(String entityType) {
        String normalized = StringUtils.hasText(entityType)
                ? entityType.trim().toLowerCase(Locale.ROOT)
                : "";
        if ("product".equals(normalized)) {
            return productRealtimeEnabled;
        }
        if ("event".equals(normalized) || "activity".equals(normalized) || "local_activity".equals(normalized)) {
            return eventRealtimeEnabled;
        }
        if ("store".equals(normalized)) {
            return storeRealtimeEnabled;
        }
        return false;
    }

    public boolean isProductRealtimeEnabled() {
        return productRealtimeEnabled;
    }

    public void setProductRealtimeEnabled(boolean productRealtimeEnabled) {
        this.productRealtimeEnabled = productRealtimeEnabled;
    }

    public boolean isEventRealtimeEnabled() {
        return eventRealtimeEnabled;
    }

    public void setEventRealtimeEnabled(boolean eventRealtimeEnabled) {
        this.eventRealtimeEnabled = eventRealtimeEnabled;
    }

    public boolean isStoreRealtimeEnabled() {
        return storeRealtimeEnabled;
    }

    public void setStoreRealtimeEnabled(boolean storeRealtimeEnabled) {
        this.storeRealtimeEnabled = storeRealtimeEnabled;
    }
}

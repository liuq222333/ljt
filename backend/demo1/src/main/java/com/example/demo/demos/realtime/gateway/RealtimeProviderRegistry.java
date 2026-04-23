package com.example.demo.demos.realtime.gateway;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RealtimeProviderRegistry {

    private final Map<String, RealtimeEntityProvider> providers;

    public RealtimeProviderRegistry(List<RealtimeEntityProvider> providers) {
        Map<String, RealtimeEntityProvider> mapped = new LinkedHashMap<String, RealtimeEntityProvider>();
        for (RealtimeEntityProvider provider : providers) {
            mapped.put(normalize(provider.getEntityType()), provider);
        }
        this.providers = Collections.unmodifiableMap(mapped);
    }

    public RealtimeEntityProvider getProvider(String entityType) {
        return providers.get(normalize(entityType));
    }

    public Map<String, Object> healthSummary() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("providerCount", providers.size());
        Map<String, Object> providerStatuses = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, RealtimeEntityProvider> entry : providers.entrySet()) {
            providerStatuses.put(entry.getKey(), entry.getValue().health());
        }
        result.put("providers", providerStatuses);
        return result;
    }

    private String normalize(String entityType) {
        return StringUtils.hasText(entityType)
                ? entityType.trim().toLowerCase(Locale.ROOT)
                : "";
    }
}

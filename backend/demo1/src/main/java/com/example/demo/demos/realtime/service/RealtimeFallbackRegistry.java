package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RealtimeFallbackRegistry {

    private final Map<String, RealtimeFallbackProvider> providers;

    public RealtimeFallbackRegistry(List<RealtimeFallbackProvider> providers) {
        Map<String, RealtimeFallbackProvider> mapped = new LinkedHashMap<String, RealtimeFallbackProvider>();
        if (providers != null) {
            for (RealtimeFallbackProvider provider : providers) {
                mapped.put(normalize(provider.getEntityType()), provider);
            }
        }
        this.providers = Collections.unmodifiableMap(mapped);
    }

    public RealtimeQueryResponse query(RealtimeQueryRequest request, List<Long> targetIds, String fallbackReason) {
        String entityType = request == null ? null : request.getEntityType();
        RealtimeFallbackProvider provider = providers.get(normalize(entityType));
        if (provider == null) {
            return unsupported(entityType, targetIds, fallbackReason);
        }
        return provider.query(request, targetIds, fallbackReason);
    }

    private RealtimeQueryResponse unsupported(String entityType, List<Long> targetIds, String fallbackReason) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.FAILED);
        response.setPartialFailedIds(targetIds == null
                ? new ArrayList<Long>()
                : new ArrayList<Long>(targetIds));
        response.getQueryMeta().put("fallbackUsed", true);
        response.getQueryMeta().put("fallbackReason", fallbackReason);
        response.getQueryMeta().put("source", "no_fallback_provider");
        response.getQueryMeta().put("unsupportedEntityType", entityType);
        response.getQueryMeta().put("fallbackProviderMissing", true);
        return response;
    }

    private String normalize(String entityType) {
        return StringUtils.hasText(entityType)
                ? entityType.trim().toLowerCase(Locale.ROOT)
                : "";
    }
}

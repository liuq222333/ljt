package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InternalRealtimeGatewayService {

    private final RealtimeProviderRegistry providerRegistry;

    public InternalRealtimeGatewayService(RealtimeProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("status", "UP");
        result.put("enabled", true);
        result.put("gatewayMode", "internal");
        Map<String, Object> summary = providerRegistry.healthSummary();
        result.put("providerSummary", summary);
        Map<String, Object> providers = castMap(summary.get("providers"));
        result.put("productProviderReady", isImplemented(providers.get("product")));
        result.put("storeProviderReady", isImplemented(providers.get("store")));
        result.put("eventProviderReady", isImplemented(providers.get("event")));
        return result;
    }

    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        if (request == null) {
            return failed("empty_request", null);
        }
        if (!StringUtils.hasText(request.getEntityType())) {
            return failed("missing_entity_type", request);
        }
        if (CollectionUtils.isEmpty(request.getEntityIds())) {
            return failed("empty_entity_ids", request);
        }
        RealtimeEntityProvider provider = providerRegistry.getProvider(request.getEntityType());
        if (provider == null) {
            return failed("unsupported_entity_type", request);
        }
        RealtimeQueryResponse response = provider.query(request);
        response.getQueryMeta().put("gatewayMode", "internal");
        response.getQueryMeta().put("provider", provider.getEntityType());
        return response;
    }

    public RealtimeBatchQueryResponse batchQuery(RealtimeBatchQueryRequest request) {
        RealtimeBatchQueryResponse response = new RealtimeBatchQueryResponse();
        List<RealtimeQueryRequest> requests = request == null || request.getRequests() == null
                ? Collections.<RealtimeQueryRequest>emptyList()
                : request.getRequests();
        for (RealtimeQueryRequest item : requests) {
            response.getResults().add(query(item));
        }
        response.getBatchMeta().put("requestCount", requests.size());
        response.getBatchMeta().put("gatewayMode", "internal");
        return response;
    }

    private RealtimeQueryResponse failed(String reason, RealtimeQueryRequest request) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        response.setRealtimeStatus(RealtimeStatus.FAILED);
        response.getQueryMeta().put("gatewayMode", "internal");
        response.getQueryMeta().put("reason", reason);
        if (request != null) {
            response.getQueryMeta().put("entityType", request.getEntityType());
            response.setPartialFailedIds(request.getEntityIds());
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    private boolean isImplemented(Object value) {
        if (!(value instanceof Map)) {
            return false;
        }
        Object implemented = ((Map<String, Object>) value).get("implemented");
        return Boolean.TRUE.equals(implemented);
    }
}

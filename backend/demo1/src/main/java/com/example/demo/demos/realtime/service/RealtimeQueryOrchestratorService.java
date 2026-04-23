package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.ratelimit.UserRateLimiter;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RealtimeQueryOrchestratorService {

    private final RealtimeQueryProperties properties;
    private final RealtimeGatewayClient realtimeGatewayClient;
    private final ProductRealtimeFallbackService productRealtimeFallbackService;
    private final UserRateLimiter userRateLimiter;
    private final Map<String, CachedRealtimeResponse> cache = new ConcurrentHashMap<String, CachedRealtimeResponse>();
    private final Object circuitLock = new Object();
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong cacheHitCount = new AtomicLong();
    private final AtomicLong gatewayRequestCount = new AtomicLong();
    private final AtomicLong gatewaySuccessCount = new AtomicLong();
    private final AtomicLong gatewayFailureCount = new AtomicLong();
    private final AtomicLong fallbackResponseCount = new AtomicLong();
    private final AtomicLong rateLimitedCount = new AtomicLong();
    private volatile int consecutiveGatewayFailures;
    private volatile long circuitOpenUntilEpochMilli;
    private volatile long lastGatewayFailureAtEpochMilli;
    private volatile long lastGatewaySuccessAtEpochMilli;
    private volatile String lastGatewayFailureReason;

    public RealtimeQueryOrchestratorService(RealtimeQueryProperties properties,
                                            RealtimeGatewayClient realtimeGatewayClient,
                                            ProductRealtimeFallbackService productRealtimeFallbackService,
                                            UserRateLimiter userRateLimiter) {
        this.properties = properties;
        this.realtimeGatewayClient = realtimeGatewayClient;
        this.productRealtimeFallbackService = productRealtimeFallbackService;
        this.userRateLimiter = userRateLimiter;
    }

    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        totalRequests.incrementAndGet();
        validateRequest(request);
        List<Long> normalizedIds = normalizeIds(request.getEntityIds());
        request.setEntityIds(normalizedIds);
        String cacheKey = buildCacheKey(request);
        if (!request.isForceRefresh()) {
            RealtimeQueryResponse cached = getCached(cacheKey);
            if (cached != null) {
                cacheHitCount.incrementAndGet();
                cached.getQueryMeta().put("cacheHit", true);
                return cached;
            }
        }
        if (StringUtils.hasText(request.getUserId())) {
            try {
                userRateLimiter.checkLimit(request.getUserId());
            } catch (BizException ex) {
                rateLimitedCount.incrementAndGet();
                throw ex;
            }
        }

        long start = System.currentTimeMillis();
        RealtimeQueryResponse result;
        if (!properties.isGatewayEnabled()) {
            result = productRealtimeFallbackService.query(request, normalizedIds, "gateway_disabled");
        } else {
            result = queryWithGateway(request, normalizedIds);
        }
        result.getQueryMeta().put("cacheHit", false);
        result.getQueryMeta().put("latencyMs", System.currentTimeMillis() - start);
        result.getQueryMeta().put("maxCandidates", properties.getMaxCandidates());
        if (shouldCache(result)) {
            putCache(cacheKey, result);
        }
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", properties.isEnabled());
        result.put("gatewayEnabled", properties.isGatewayEnabled());
        result.put("mockGatewayEnabled", properties.isMockGatewayEnabled());
        result.put("cacheSize", cache.size());
        result.put("cacheTtlSeconds", properties.getCacheTtlSeconds());
        result.put("maxCandidates", properties.getMaxCandidates());
        result.put("metrics", buildMetrics());
        result.put("circuit", buildCircuitStatus());
        return result;
    }

    public Map<String, Object> clearCache() {
        int cleared = cache.size();
        cache.clear();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("clearedEntries", cleared);
        result.put("cacheSize", cache.size());
        return result;
    }

    public Map<String, Object> resetCircuit() {
        synchronized (circuitLock) {
            consecutiveGatewayFailures = 0;
            circuitOpenUntilEpochMilli = 0L;
            lastGatewayFailureReason = null;
        }
        return buildCircuitStatus();
    }

    public Map<String, Object> forceOpenCircuit(int durationSeconds, String reason) {
        int safeDuration = durationSeconds <= 0 ? 30 : durationSeconds;
        synchronized (circuitLock) {
            consecutiveGatewayFailures = Math.max(consecutiveGatewayFailures, properties.getCircuitFailureThreshold());
            circuitOpenUntilEpochMilli = Instant.now().toEpochMilli() + safeDuration * 1000L;
            lastGatewayFailureAtEpochMilli = Instant.now().toEpochMilli();
            lastGatewayFailureReason = StringUtils.hasText(reason) ? reason : "manual_force_open";
        }
        Map<String, Object> result = buildCircuitStatus();
        result.put("forced", true);
        result.put("durationSeconds", safeDuration);
        result.put("reason", lastGatewayFailureReason);
        return result;
    }

    public Map<String, Object> smokeGateway(RealtimeQueryRequest request) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("gatewayEnabled", properties.isGatewayEnabled());
        result.put("circuit", buildCircuitStatus());
        if (!properties.isGatewayEnabled()) {
            result.put("success", false);
            result.put("reason", "gateway_disabled");
            return result;
        }
        RealtimeQueryRequest effectiveRequest = request == null ? new RealtimeQueryRequest() : request;
        if (!StringUtils.hasText(effectiveRequest.getEntityType())) {
            effectiveRequest.setEntityType("product");
        }
        if (CollectionUtils.isEmpty(effectiveRequest.getEntityIds())) {
            effectiveRequest.setEntityIds(Collections.singletonList(1L));
        }
        if (!StringUtils.hasText(effectiveRequest.getQueryType())) {
            effectiveRequest.setQueryType("availability");
        }
        long start = System.currentTimeMillis();
        try {
            RealtimeQueryResponse response = realtimeGatewayClient.query(effectiveRequest);
            RealtimeStatus status = response == null ? RealtimeStatus.FAILED : response.getRealtimeStatus();
            boolean success = status == RealtimeStatus.SUCCESS || status == RealtimeStatus.PARTIAL_SUCCESS;
            result.put("success", success);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("realtimeStatus", status.getCode());
            result.put("itemCount", response == null ? 0 : response.getItems().size());
            result.put("partialFailedIds", response == null ? Collections.emptyList() : response.getPartialFailedIds());
            result.put("queryMeta", response == null ? Collections.emptyMap() : response.getQueryMeta());
            if (!success) {
                result.put("reason", "gateway_" + status.getCode());
            }
            return result;
        } catch (BizException ex) {
            result.put("success", false);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("errorCode", ex.getCode());
            result.put("errorMessage", ex.getMessage());
            return result;
        }
    }

    private RealtimeQueryResponse queryWithGateway(RealtimeQueryRequest request, List<Long> normalizedIds) {
        if (isCircuitOpen()) {
            fallbackResponseCount.incrementAndGet();
            RealtimeQueryResponse fallback = productRealtimeFallbackService.query(request, normalizedIds, "circuit_open");
            fallback.getQueryMeta().put("gatewaySkipped", true);
            fallback.getQueryMeta().put("circuit", buildCircuitStatus());
            return fallback;
        }
        RealtimeQueryResponse gatewayResponse;
        gatewayRequestCount.incrementAndGet();
        try {
            gatewayResponse = realtimeGatewayClient.query(request);
        } catch (BizException ex) {
            recordGatewayFailure(codeToReason(ex.getCode()));
            fallbackResponseCount.incrementAndGet();
            RealtimeQueryResponse fallback = productRealtimeFallbackService.query(request, normalizedIds, codeToReason(ex.getCode()));
            fallback.getQueryMeta().put("circuit", buildCircuitStatus());
            return fallback;
        }
        if (gatewayResponse == null) {
            recordGatewayFailure("gateway_null_response");
            fallbackResponseCount.incrementAndGet();
            RealtimeQueryResponse fallback = productRealtimeFallbackService.query(request, normalizedIds, "gateway_null_response");
            fallback.getQueryMeta().put("circuit", buildCircuitStatus());
            return fallback;
        }
        RealtimeStatus gatewayStatus = gatewayResponse.getRealtimeStatus() == null
                ? RealtimeStatus.FAILED
                : gatewayResponse.getRealtimeStatus();
        gatewayResponse.setRealtimeStatus(gatewayStatus);
        gatewayResponse.getQueryMeta().put("gatewayEnabled", true);
        gatewayResponse.getQueryMeta().put("circuit", buildCircuitStatus());
        if (gatewayStatus == RealtimeStatus.FAILED
                || gatewayStatus == RealtimeStatus.TIMEOUT
                || gatewayStatus == RealtimeStatus.DEGRADED) {
            recordGatewayFailure("gateway_status_" + gatewayStatus.getCode());
            fallbackResponseCount.incrementAndGet();
            RealtimeQueryResponse fallback = productRealtimeFallbackService.query(request, normalizedIds, gatewayStatus.getCode());
            fallback.getQueryMeta().put("circuit", buildCircuitStatus());
            return fallback;
        }

        recordGatewaySuccess();
        if (gatewayStatus == RealtimeStatus.SUCCESS && coversAllIds(gatewayResponse, normalizedIds)) {
            return gatewayResponse;
        }

        List<Long> missingIds = findMissingIds(gatewayResponse, normalizedIds);
        if (missingIds.isEmpty() && gatewayStatus == RealtimeStatus.SUCCESS) {
            return gatewayResponse;
        }
        RealtimeQueryResponse fallbackResponse = productRealtimeFallbackService.query(
                request,
                missingIds.isEmpty() ? normalizedIds : missingIds,
                gatewayStatus.getCode()
        );
        fallbackResponseCount.incrementAndGet();
        return merge(gatewayResponse, fallbackResponse, normalizedIds);
    }

    private RealtimeQueryResponse merge(RealtimeQueryResponse gatewayResponse,
                                        RealtimeQueryResponse fallbackResponse,
                                        List<Long> normalizedIds) {
        Map<String, RealtimeResultItem> merged = new LinkedHashMap<String, RealtimeResultItem>();
        for (RealtimeResultItem item : gatewayResponse.getItems()) {
            merged.put(item.getEntityId(), item);
        }
        for (RealtimeResultItem item : fallbackResponse.getItems()) {
            if (!merged.containsKey(item.getEntityId())) {
                merged.put(item.getEntityId(), item);
            }
        }
        RealtimeQueryResponse result = new RealtimeQueryResponse();
        result.setItems(new ArrayList<RealtimeResultItem>(merged.values()));
        result.setPartialFailedIds(findMissingIds(result, normalizedIds));
        if (gatewayResponse.getRealtimeStatus() == RealtimeStatus.SUCCESS && result.getPartialFailedIds().isEmpty()) {
            result.setRealtimeStatus(RealtimeStatus.SUCCESS);
        } else if (!gatewayResponse.getItems().isEmpty() && !fallbackResponse.getItems().isEmpty()) {
            result.setRealtimeStatus(RealtimeStatus.PARTIAL_SUCCESS);
        } else if (!fallbackResponse.getItems().isEmpty()) {
            result.setRealtimeStatus(RealtimeStatus.DEGRADED);
        } else {
            result.setRealtimeStatus(gatewayResponse.getRealtimeStatus());
        }
        result.getQueryMeta().put("gatewayStatus", gatewayResponse.getRealtimeStatus().getCode());
        result.getQueryMeta().put("fallbackUsed", !fallbackResponse.getItems().isEmpty());
        result.getQueryMeta().put("fallbackStatus", fallbackResponse.getRealtimeStatus().getCode());
        result.getQueryMeta().put("gatewayMeta", gatewayResponse.getQueryMeta());
        result.getQueryMeta().put("fallbackMeta", fallbackResponse.getQueryMeta());
        result.getQueryMeta().put("circuit", buildCircuitStatus());
        return result;
    }

    private void validateRequest(RealtimeQueryRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.REALTIME_RESPONSE_ERROR, "实时查询请求不能为空");
        }
        if (!properties.isEnabled()) {
            throw new BizException(ErrorCode.REALTIME_UNAVAILABLE, "实时查询功能未启用");
        }
        if (!StringUtils.hasText(request.getEntityType())) {
            throw new BizException(ErrorCode.REALTIME_RESPONSE_ERROR, "entityType 不能为空");
        }
        if (CollectionUtils.isEmpty(request.getEntityIds())) {
            throw new BizException(ErrorCode.REALTIME_RESPONSE_ERROR, "entityIds 不能为空");
        }
    }

    private List<Long> normalizeIds(List<Long> rawIds) {
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        for (Long rawId : rawIds) {
            if (rawId != null) {
                ids.add(rawId);
            }
            if (ids.size() >= properties.getMaxCandidates()) {
                break;
            }
        }
        return new ArrayList<Long>(ids);
    }

    private String buildCacheKey(RealtimeQueryRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getEntityType()).append('|')
                .append(request.getQueryType()).append('|')
                .append(request.getDate()).append('|')
                .append(request.getTimeSlot()).append('|');
        for (Long entityId : request.getEntityIds()) {
            builder.append(entityId).append(',');
        }
        return builder.toString();
    }

    private RealtimeQueryResponse getCached(String cacheKey) {
        CachedRealtimeResponse cached = cache.get(cacheKey);
        if (cached == null) {
            return null;
        }
        if (cached.expireEpochMilli < Instant.now().toEpochMilli()) {
            cache.remove(cacheKey);
            return null;
        }
        return cloneResponse(cached.response);
    }

    private void putCache(String cacheKey, RealtimeQueryResponse response) {
        cache.put(cacheKey, new CachedRealtimeResponse(
                cloneResponse(response),
                Instant.now().toEpochMilli() + properties.getCacheTtlSeconds() * 1000L
        ));
    }

    private boolean shouldCache(RealtimeQueryResponse response) {
        return response != null && response.getRealtimeStatus() == RealtimeStatus.SUCCESS;
    }

    private boolean coversAllIds(RealtimeQueryResponse response, List<Long> normalizedIds) {
        return findMissingIds(response, normalizedIds).isEmpty();
    }

    private List<Long> findMissingIds(RealtimeQueryResponse response, List<Long> normalizedIds) {
        Set<Long> existing = new LinkedHashSet<Long>();
        for (RealtimeResultItem item : response.getItems()) {
            if (StringUtils.hasText(item.getEntityId())) {
                try {
                    existing.add(Long.valueOf(item.getEntityId()));
                } catch (NumberFormatException ignore) {
                    // Ignore malformed gateway IDs and let them fall through as missing.
                }
            }
        }
        List<Long> missing = new ArrayList<Long>();
        for (Long entityId : normalizedIds) {
            if (!existing.contains(entityId)) {
                missing.add(entityId);
            }
        }
        return missing;
    }

    private String codeToReason(int code) {
        if (code == ErrorCode.REALTIME_TIMEOUT) {
            return "gateway_timeout";
        }
        if (code == ErrorCode.REALTIME_UNAVAILABLE) {
            return "gateway_unavailable";
        }
        return "gateway_error";
    }

    private void recordGatewaySuccess() {
        gatewaySuccessCount.incrementAndGet();
        synchronized (circuitLock) {
            consecutiveGatewayFailures = 0;
            circuitOpenUntilEpochMilli = 0L;
            lastGatewayFailureReason = null;
            lastGatewaySuccessAtEpochMilli = Instant.now().toEpochMilli();
        }
    }

    private void recordGatewayFailure(String reason) {
        gatewayFailureCount.incrementAndGet();
        synchronized (circuitLock) {
            consecutiveGatewayFailures++;
            lastGatewayFailureReason = reason;
            lastGatewayFailureAtEpochMilli = Instant.now().toEpochMilli();
            if (consecutiveGatewayFailures >= Math.max(1, properties.getCircuitFailureThreshold())) {
                circuitOpenUntilEpochMilli = Instant.now().toEpochMilli()
                        + Math.max(1, properties.getCircuitOpenSeconds()) * 1000L;
            }
        }
    }

    private boolean isCircuitOpen() {
        return circuitOpenUntilEpochMilli > Instant.now().toEpochMilli();
    }

    private Map<String, Object> buildMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<String, Object>();
        metrics.put("totalRequests", totalRequests.get());
        metrics.put("cacheHitCount", cacheHitCount.get());
        metrics.put("gatewayRequestCount", gatewayRequestCount.get());
        metrics.put("gatewaySuccessCount", gatewaySuccessCount.get());
        metrics.put("gatewayFailureCount", gatewayFailureCount.get());
        metrics.put("fallbackResponseCount", fallbackResponseCount.get());
        metrics.put("rateLimitedCount", rateLimitedCount.get());
        return metrics;
    }

    private Map<String, Object> buildCircuitStatus() {
        Map<String, Object> circuit = new LinkedHashMap<String, Object>();
        long now = Instant.now().toEpochMilli();
        long remainingMs = Math.max(0L, circuitOpenUntilEpochMilli - now);
        circuit.put("state", remainingMs > 0 ? "OPEN" : "CLOSED");
        circuit.put("remainingOpenMs", remainingMs);
        circuit.put("failureThreshold", properties.getCircuitFailureThreshold());
        circuit.put("openSeconds", properties.getCircuitOpenSeconds());
        circuit.put("consecutiveFailures", consecutiveGatewayFailures);
        circuit.put("lastFailureReason", lastGatewayFailureReason);
        circuit.put("lastFailureAt", lastGatewayFailureAtEpochMilli);
        circuit.put("lastSuccessAt", lastGatewaySuccessAtEpochMilli);
        return circuit;
    }

    private RealtimeQueryResponse cloneResponse(RealtimeQueryResponse source) {
        RealtimeQueryResponse target = new RealtimeQueryResponse();
        target.setRealtimeStatus(source.getRealtimeStatus());
        target.setPartialFailedIds(new ArrayList<Long>(source.getPartialFailedIds()));
        target.setQueryMeta(new LinkedHashMap<String, Object>(source.getQueryMeta()));
        List<RealtimeResultItem> clonedItems = new ArrayList<RealtimeResultItem>();
        for (RealtimeResultItem item : source.getItems()) {
            RealtimeResultItem clone = new RealtimeResultItem();
            clone.setEntityId(item.getEntityId());
            clone.setInventoryStatus(item.getInventoryStatus());
            clone.setInventoryCount(item.getInventoryCount());
            clone.setSellStatus(item.getSellStatus());
            clone.setAvailabilityStatus(item.getAvailabilityStatus());
            clone.setBookable(item.getBookable());
            clone.setPrice(item.getPrice());
            clone.setCurrency(item.getCurrency());
            clone.setBusinessStatus(item.getBusinessStatus());
            clone.setOpenNow(item.getOpenNow());
            clone.setQueryTs(item.getQueryTs());
            clone.setSuccess(item.isSuccess());
            clone.setDegraded(item.isDegraded());
            clone.setSource(item.getSource());
            clone.setErrorCode(item.getErrorCode());
            clone.setErrorMessage(item.getErrorMessage());
            clonedItems.add(clone);
        }
        target.setItems(clonedItems);
        return target;
    }

    private static class CachedRealtimeResponse {
        private final RealtimeQueryResponse response;
        private final long expireEpochMilli;

        private CachedRealtimeResponse(RealtimeQueryResponse response, long expireEpochMilli) {
            this.response = response;
            this.expireEpochMilli = expireEpochMilli;
        }
    }
}

package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.ratelimit.UserRateLimiter;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.config.RealtimeToolRegistryProperties;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RealtimeQueryOrchestratorService {

    private final RealtimeQueryProperties properties;
    private final RealtimeGatewayClient realtimeGatewayClient;
    private final RealtimeFallbackRegistry fallbackRegistry;
    private final UserRateLimiter userRateLimiter;
    private final RealtimeToolRegistryProperties toolRegistryProperties;
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
                                            RealtimeFallbackRegistry fallbackRegistry,
                                            UserRateLimiter userRateLimiter,
                                            RealtimeToolRegistryProperties toolRegistryProperties) {
        this.properties = properties;
        this.realtimeGatewayClient = realtimeGatewayClient;
        this.fallbackRegistry = fallbackRegistry;
        this.userRateLimiter = userRateLimiter;
        this.toolRegistryProperties = toolRegistryProperties;
    }

    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        totalRequests.incrementAndGet();
        validateRequest(request);
        RealtimeQueryRequest effectiveRequest = copyRequest(request);
        List<Long> normalizedIds = normalizeIds(effectiveRequest.getEntityIds());
        effectiveRequest.setEntityIds(normalizedIds);
        String cacheKey = buildCacheKey(effectiveRequest);
        if (!effectiveRequest.isForceRefresh()) {
            RealtimeQueryResponse cached = getCached(cacheKey);
            if (cached != null) {
                cacheHitCount.incrementAndGet();
                cached.getQueryMeta().put("cacheHit", true);
                return cached;
            }
        }
        if (StringUtils.hasText(effectiveRequest.getUserId())) {
            try {
                userRateLimiter.checkLimit(effectiveRequest.getUserId());
            } catch (BizException ex) {
                rateLimitedCount.incrementAndGet();
                throw ex;
            }
        }

        long start = System.currentTimeMillis();
        RealtimeQueryResponse result;
        if (!properties.isGatewayEnabled()) {
            result = fallbackRegistry.query(effectiveRequest, normalizedIds, "gateway_disabled");
        } else {
            result = queryWithGateway(effectiveRequest, normalizedIds);
        }
        result.getQueryMeta().put("cacheHit", false);
        result.getQueryMeta().put("latencyMs", System.currentTimeMillis() - start);
        result.getQueryMeta().put("maxCandidates", properties.getMaxCandidates());
        if (shouldCache(result)) {
            putCache(cacheKey, result, cacheTtlSecondsFor(result));
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
        result.put("partialCacheTtlSeconds", properties.getPartialCacheTtlSeconds());
        result.put("degradedCacheTtlSeconds", properties.getDegradedCacheTtlSeconds());
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
        RealtimeQueryRequest effectiveRequest = request == null ? new RealtimeQueryRequest() : copyRequest(request);
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
            RealtimeQueryResponse fallback = fallbackRegistry.query(request, normalizedIds, "circuit_open");
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
            RealtimeQueryResponse fallback = fallbackRegistry.query(request, normalizedIds, codeToReason(ex.getCode()));
            fallback.getQueryMeta().put("circuit", buildCircuitStatus());
            return fallback;
        }
        if (gatewayResponse == null) {
            recordGatewayFailure("gateway_null_response");
            fallbackResponseCount.incrementAndGet();
            RealtimeQueryResponse fallback = fallbackRegistry.query(request, normalizedIds, "gateway_null_response");
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
            RealtimeQueryResponse fallback = fallbackRegistry.query(request, normalizedIds, gatewayStatus.getCode());
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
        RealtimeQueryResponse fallbackResponse = fallbackRegistry.query(
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
        RealtimeStatus gatewayStatus = gatewayResponse.getRealtimeStatus() == null
                ? RealtimeStatus.FAILED
                : gatewayResponse.getRealtimeStatus();
        RealtimeStatus fallbackStatus = fallbackResponse.getRealtimeStatus() == null
                ? RealtimeStatus.FAILED
                : fallbackResponse.getRealtimeStatus();
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
        boolean fallbackUsed = !fallbackResponse.getItems().isEmpty();
        boolean gatewayHasItems = !gatewayResponse.getItems().isEmpty();
        boolean hasAnyItems = !result.getItems().isEmpty();
        if (!result.getPartialFailedIds().isEmpty()) {
            result.setRealtimeStatus(hasAnyItems ? RealtimeStatus.PARTIAL_SUCCESS : RealtimeStatus.FAILED);
        } else if (gatewayStatus == RealtimeStatus.SUCCESS && !fallbackUsed) {
            result.setRealtimeStatus(RealtimeStatus.SUCCESS);
        } else if (gatewayHasItems && fallbackUsed) {
            result.setRealtimeStatus(RealtimeStatus.PARTIAL_SUCCESS);
        } else if (fallbackUsed) {
            result.setRealtimeStatus(RealtimeStatus.DEGRADED);
        } else {
            result.setRealtimeStatus(gatewayStatus);
        }
        if (result.getRealtimeStatus() == RealtimeStatus.SUCCESS && !result.getPartialFailedIds().isEmpty()) {
            result.setRealtimeStatus(hasAnyItems ? RealtimeStatus.PARTIAL_SUCCESS : RealtimeStatus.FAILED);
        }
        result.getQueryMeta().put("gatewayStatus", gatewayStatus.getCode());
        result.getQueryMeta().put("fallbackUsed", fallbackUsed);
        result.getQueryMeta().put("mergedFromFallback", fallbackUsed);
        result.getQueryMeta().put("complete", result.getPartialFailedIds().isEmpty());
        result.getQueryMeta().put("fallbackStatus", fallbackStatus.getCode());
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
        if (!toolRegistryProperties.isRealtimeEnabled(request.getEntityType())) {
            throw new BizException(
                    ErrorCode.REALTIME_UNAVAILABLE,
                    normalizeEntityType(request.getEntityType()) + " 实时查询未启用"
            );
        }
        if (CollectionUtils.isEmpty(request.getEntityIds())) {
            throw new BizException(ErrorCode.REALTIME_RESPONSE_ERROR, "entityIds 不能为空");
        }
    }

    private RealtimeQueryRequest copyRequest(RealtimeQueryRequest source) {
        RealtimeQueryRequest target = new RealtimeQueryRequest();
        target.setEntityType(source.getEntityType());
        target.setEntityIds(source.getEntityIds() == null
                ? new ArrayList<Long>()
                : new ArrayList<Long>(source.getEntityIds()));
        target.setQueryType(source.getQueryType());
        target.setDate(source.getDate());
        target.setTimeSlot(source.getTimeSlot());
        target.setTraceId(source.getTraceId());
        target.setTimeoutMs(source.getTimeoutMs());
        target.setUserId(source.getUserId());
        target.setForceRefresh(source.isForceRefresh());
        return target;
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
        builder.append(normalizeEntityType(request.getEntityType())).append('|')
                .append(request.getQueryType()).append('|')
                .append(request.getDate()).append('|')
                .append(request.getTimeSlot()).append('|');
        List<Long> sortedIds = new ArrayList<Long>(request.getEntityIds());
        Collections.sort(sortedIds);
        for (Long entityId : sortedIds) {
            builder.append(entityId).append(',');
        }
        return builder.toString();
    }

    private String normalizeEntityType(String entityType) {
        return StringUtils.hasText(entityType)
                ? entityType.trim().toLowerCase(Locale.ROOT)
                : "";
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

    private void putCache(String cacheKey, RealtimeQueryResponse response, int ttlSeconds) {
        cache.put(cacheKey, new CachedRealtimeResponse(
                cloneResponse(response),
                Instant.now().toEpochMilli() + Math.max(1, ttlSeconds) * 1000L
        ));
    }

    private boolean shouldCache(RealtimeQueryResponse response) {
        return cacheTtlSecondsFor(response) > 0;
    }

    private int cacheTtlSecondsFor(RealtimeQueryResponse response) {
        if (response == null || response.getRealtimeStatus() == null) {
            return 0;
        }
        if (response.getRealtimeStatus() == RealtimeStatus.SUCCESS) {
            return Math.max(0, properties.getCacheTtlSeconds());
        }
        if (response.getRealtimeStatus() == RealtimeStatus.PARTIAL_SUCCESS) {
            return Math.max(0, properties.getPartialCacheTtlSeconds());
        }
        if (response.getRealtimeStatus() == RealtimeStatus.DEGRADED) {
            return Math.max(0, properties.getDegradedCacheTtlSeconds());
        }
        return 0;
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

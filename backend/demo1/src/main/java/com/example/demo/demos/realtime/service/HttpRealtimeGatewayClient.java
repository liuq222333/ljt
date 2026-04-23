package com.example.demo.demos.realtime.service;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.common.error.BizException;
import com.example.demo.demos.common.error.ErrorCode;
import com.example.demo.demos.common.retry.RetryHelper;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

@Component
@ConditionalOnProperty(prefix = "agent.realtime", name = "mock-gateway-enabled", havingValue = "false", matchIfMissing = true)
public class HttpRealtimeGatewayClient implements RealtimeGatewayClient {

    private final RealtimeQueryProperties properties;
    private final RestTemplate restTemplate;

    public HttpRealtimeGatewayClient(RealtimeQueryProperties properties,
                                     RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getGatewayConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getGatewayReadTimeoutMs()))
                .build();
    }

    @Override
    public RealtimeQueryResponse query(final RealtimeQueryRequest request) {
        if (!properties.isGatewayEnabled() || !StringUtils.hasText(properties.getGatewayBaseUrl())) {
            RealtimeQueryResponse disabled = new RealtimeQueryResponse();
            disabled.setRealtimeStatus(RealtimeStatus.DEGRADED);
            disabled.getQueryMeta().put("gatewayEnabled", false);
            disabled.getQueryMeta().put("reason", "gateway_disabled");
            return disabled;
        }
        try {
            return RetryHelper.execute(() -> doQuery(request),
                    Math.max(properties.getGatewayRetryCount(), 0),
                    50L,
                    "realtime_gateway_query");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex instanceof ResourceAccessException) {
                throw new BizException(ErrorCode.REALTIME_TIMEOUT, "实时 gateway 超时", ex);
            }
            throw new BizException(ErrorCode.REALTIME_UNAVAILABLE, "实时 gateway 不可用", ex);
        }
    }

    private RealtimeQueryResponse doQuery(RealtimeQueryRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<RealtimeQueryRequest> entity = new HttpEntity<RealtimeQueryRequest>(request, headers);
        try {
            ResponseEntity<RealtimeQueryResponse> response = restTemplate.postForEntity(
                    normalizeUrl(properties.getGatewayBaseUrl(), properties.getGatewayQueryPath()),
                    entity,
                    RealtimeQueryResponse.class
            );
            RealtimeQueryResponse body = response.getBody();
            if (body == null) {
                throw new BizException(ErrorCode.REALTIME_RESPONSE_ERROR, "实时 gateway 返回空响应");
            }
            body.getQueryMeta().put("gatewayEnabled", true);
            body.getQueryMeta().put("gatewayUrl", normalizeUrl(properties.getGatewayBaseUrl(), properties.getGatewayQueryPath()));
            return body;
        } catch (ResourceAccessException ex) {
            throw new BizException(ErrorCode.REALTIME_TIMEOUT, "实时 gateway 超时", ex);
        } catch (RestClientException ex) {
            throw new BizException(ErrorCode.REALTIME_UNAVAILABLE, "实时 gateway 调用失败", ex);
        }
    }

    private String normalizeUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String normalizedPath = StringUtils.hasText(path) ? path.trim() : "/query";
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return base + normalizedPath;
    }
}

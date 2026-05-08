package com.example.demo.demos.Agent.Service;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BackendApiProxyService {

    private final ApiRouteService apiRouteService;
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public BackendApiProxyService(ApiRouteService apiRouteService,
                                  RestTemplateBuilder restTemplateBuilder,
                                  @Value("${agent.backend-tool.base-url:}") String configuredBaseUrl,
                                  @Value("${server.port:8080}") int serverPort) {
        this.apiRouteService = apiRouteService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.baseUrl = StringUtils.hasText(configuredBaseUrl)
                ? trimTrailingSlash(configuredBaseUrl)
                : "http://127.0.0.1:" + serverPort;
    }

    public InvocationResult invoke(InvocationRequest request, String authorization) {
        InvocationResult result = new InvocationResult();
        if (request == null || !StringUtils.hasText(request.getResource()) || !StringUtils.hasText(request.getAction())) {
            result.setPresentationHint("invalid_request");
            result.setData(message("Missing resource/action"));
            return result;
        }

        ApiRoute route = apiRouteService.findEnabledRoute(request.getResource(), request.getAction());
        if (route == null) {
            result.setPresentationHint("route_missing");
            result.setData(message("No enabled route found"));
            return result;
        }

        try {
            String resolvedPath = resolvePath(route.getPathTemplate(), request);
            URI uri = buildUri(resolvedPath, request.getParams());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = StringUtils.hasText(request.getAuthorization()) ? request.getAuthorization() : authorization;
            if (StringUtils.hasText(token)) {
                headers.set(HttpHeaders.AUTHORIZATION, token);
            }
            HttpMethod method = HttpMethod.resolve(route.getHttpMethod());
            if (method == null) {
                method = HttpMethod.GET;
            }
            HttpEntity<Object> entity = new HttpEntity<Object>(request.getPayload(), headers);
            ResponseEntity<Object> response = restTemplate.exchange(uri, method, entity, Object.class);
            result.setPresentationHint("direct_response");
            result.setData(response.getBody());
            return result;
        } catch (Exception ex) {
            result.setPresentationHint("proxy_error");
            result.setData(message(ex.getMessage()));
            return result;
        }
    }

    private URI buildUri(String path, Map<String, Object> params) {
        String target = path;
        if (StringUtils.hasText(target) && target.startsWith("/")) {
            target = baseUrl + target;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(target);
        if (!CollectionUtils.isEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() != null) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build(false).encode().toUri();
    }

    private String resolvePath(String pathTemplate, InvocationRequest request) {
        String resolved = StringUtils.hasText(pathTemplate) ? pathTemplate : "/";
        if (request.getId() != null && resolved.contains("{id}")) {
            resolved = resolved.replace("{id}", String.valueOf(request.getId()));
        }
        if (!CollectionUtils.isEmpty(request.getPathVariables())) {
            for (Map.Entry<String, Object> entry : request.getPathVariables().entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return resolved;
    }

    private Map<String, Object> message(String text) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("message", text);
        return result;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Data
    public static class InvocationRequest {
        private String resource;
        private String action;
        private String id;
        private Map<String, Object> pathVariables = new LinkedHashMap<String, Object>();
        private Map<String, Object> params = new LinkedHashMap<String, Object>();
        private Object payload;
        private String authorization;
    }

    @Data
    public static class InvocationResult {
        private String presentationHint;
        private Object data;
    }
}

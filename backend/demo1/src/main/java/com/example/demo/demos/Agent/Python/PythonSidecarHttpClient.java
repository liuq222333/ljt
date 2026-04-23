package com.example.demo.demos.Agent.Python;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

@Component
public class PythonSidecarHttpClient {

    private final RestTemplateBuilder restTemplateBuilder;

    public PythonSidecarHttpClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public <T> T post(String baseUrl,
                      String path,
                      Object request,
                      Class<T> responseType,
                      long connectTimeoutMs,
                      long readTimeoutMs) {
        String url = normalizeUrl(baseUrl, path);
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<T> response = restTemplate.postForEntity(url, new HttpEntity<Object>(request, headers), responseType);
            T body = response.getBody();
            if (body == null) {
                throw new PythonSidecarException("Python sidecar returned empty body: " + url);
            }
            return body;
        } catch (ResourceAccessException ex) {
            throw new PythonSidecarException("Python sidecar timeout: " + url, ex);
        } catch (RestClientResponseException ex) {
            throw new PythonSidecarException("Python sidecar HTTP " + ex.getRawStatusCode() + ": " + url, ex);
        } catch (RestClientException ex) {
            throw new PythonSidecarException("Python sidecar request failed: " + url, ex);
        }
    }

    String normalizeUrl(String baseUrl, String path) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new PythonSidecarException("Python sidecar base URL is empty");
        }
        String normalizedBaseUrl = baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        String normalizedPath = StringUtils.hasText(path) ? path.trim() : "/";
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return normalizedBaseUrl + normalizedPath;
    }
}

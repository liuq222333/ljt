package com.example.demo.demos.search.es;

import com.example.demo.demos.search.config.SearchEsProperties;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ProductSearchEsClient {

    private final SearchEsProperties properties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final RestTemplate restTemplate;

    public ProductSearchEsClient(SearchEsProperties properties,
                                 ObjectMapper objectMapper,
                                 ResourceLoader resourceLoader) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.restTemplate = new RestTemplate();
    }

    public boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getBaseUrl());
    }

    public boolean ping() {
        if (!isEnabled()) {
            return false;
        }
        try {
            ResponseEntity<String> response = exchange("/", HttpMethod.GET, null);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            return false;
        }
    }

    public boolean indexExists(String indexName) {
        try {
            ResponseEntity<String> response = exchange("/" + indexName, HttpMethod.HEAD, null);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound notFound) {
            return false;
        } catch (RestClientException ex) {
            throw new IllegalStateException("检查 ES 索引失败: " + indexName, ex);
        }
    }

    public boolean aliasExists(String aliasName) {
        if (!StringUtils.hasText(aliasName)) {
            return false;
        }
        try {
            ResponseEntity<String> response = exchange("/_alias/" + aliasName, HttpMethod.GET, null);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound notFound) {
            return false;
        } catch (RestClientException ex) {
            throw new IllegalStateException("检查 ES alias 失败: " + aliasName, ex);
        }
    }

    public void createIndex(String indexName, String body) {
        exchange("/" + indexName, HttpMethod.PUT, body);
    }

    public void deleteIndex(String indexName) {
        try {
            exchange("/" + indexName, HttpMethod.DELETE, null);
        } catch (HttpClientErrorException.NotFound ignore) {
            // ignore
        }
    }

    public List<String> resolveAliasTargets(String aliasName) {
        if (!StringUtils.hasText(aliasName)) {
            return Collections.emptyList();
        }
        try {
            ResponseEntity<String> response = exchange("/_alias/" + aliasName, HttpMethod.GET, null);
            Map<String, Object> payload = readObject(response.getBody());
            return new ArrayList<String>(payload.keySet());
        } catch (HttpClientErrorException.NotFound notFound) {
            return Collections.emptyList();
        } catch (RestClientException ex) {
            throw new IllegalStateException("读取 ES alias 失败: " + aliasName, ex);
        }
    }

    public void swapAlias(String aliasName, String targetIndex) {
        List<String> currentTargets = resolveAliasTargets(aliasName);
        List<Map<String, Object>> actions = new ArrayList<Map<String, Object>>();
        for (String currentTarget : currentTargets) {
            if (!targetIndex.equals(currentTarget)) {
                Map<String, Object> remove = new LinkedHashMap<String, Object>();
                remove.put("index", currentTarget);
                remove.put("alias", aliasName);
                Map<String, Object> action = new LinkedHashMap<String, Object>();
                action.put("remove", remove);
                actions.add(action);
            }
        }
        Map<String, Object> add = new LinkedHashMap<String, Object>();
        add.put("index", targetIndex);
        add.put("alias", aliasName);
        Map<String, Object> addAction = new LinkedHashMap<String, Object>();
        addAction.put("add", add);
        actions.add(addAction);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("actions", actions);
        exchange("/_aliases", HttpMethod.POST, writeJson(body));
    }

    public long countDocuments(String indexOrAlias) {
        try {
            ResponseEntity<String> response = exchange("/" + indexOrAlias + "/_count", HttpMethod.GET, null);
            Map<String, Object> payload = readObject(response.getBody());
            Object value = payload.get("count");
            return value instanceof Number ? ((Number) value).longValue() : 0L;
        } catch (HttpClientErrorException.NotFound notFound) {
            return 0L;
        } catch (RestClientException ex) {
            throw new IllegalStateException("查询 ES 文档量失败: " + indexOrAlias, ex);
        }
    }

    public Set<Long> listIndexedProductIds(String indexOrAlias, int limit) {
        if (!StringUtils.hasText(indexOrAlias)) {
            return Collections.emptySet();
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("size", Math.max(limit, 1));
        body.put("track_total_hits", true);
        body.put("_source", Collections.singletonList("product_id"));
        body.put("sort", Collections.singletonList(Collections.singletonMap("product_id", "asc")));
        body.put("query", Collections.singletonMap("match_all", Collections.emptyMap()));
        try {
            ResponseEntity<String> response = exchange("/" + indexOrAlias + "/_search", HttpMethod.POST, writeJson(body));
            Map<String, Object> payload = readObject(response.getBody());
            Map<String, Object> hits = asMap(payload.get("hits"));
            List<Map<String, Object>> rows = asListOfMap(hits.get("hits"));
            Set<Long> ids = new LinkedHashSet<Long>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> source = asMap(row.get("_source"));
                Object productId = source.get("product_id");
                if (productId instanceof Number) {
                    ids.add(((Number) productId).longValue());
                } else if (productId instanceof String && StringUtils.hasText((String) productId)) {
                    ids.add(Long.valueOf((String) productId));
                }
            }
            return ids;
        } catch (HttpClientErrorException.NotFound notFound) {
            return Collections.emptySet();
        } catch (RestClientException ex) {
            throw new IllegalStateException("查询 ES 商品 ID 失败: " + indexOrAlias, ex);
        }
    }

    public void bulkUpsert(String indexOrAlias, List<ProductSearchSnapshot> snapshots) {
        if (CollectionUtils.isEmpty(snapshots)) {
            return;
        }
        StringBuilder bulk = new StringBuilder();
        for (ProductSearchSnapshot snapshot : snapshots) {
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            Map<String, Object> index = new LinkedHashMap<String, Object>();
            index.put("_index", indexOrAlias);
            index.put("_id", String.valueOf(snapshot.getProductId()));
            metadata.put("index", index);
            bulk.append(writeJson(metadata)).append('\n');
            bulk.append(writeJson(buildDocument(snapshot))).append('\n');
        }
        ResponseEntity<String> response = exchange("/_bulk", HttpMethod.POST, bulk.toString(), "application/x-ndjson");
        Map<String, Object> payload = readObject(response.getBody());
        if (Boolean.TRUE.equals(payload.get("errors"))) {
            throw new IllegalStateException("ES bulk upsert 返回 errors=true");
        }
    }

    public void deleteDocument(String indexOrAlias, Long productId) {
        if (productId == null) {
            return;
        }
        try {
            exchange("/" + indexOrAlias + "/_doc/" + productId, HttpMethod.DELETE, null);
        } catch (HttpClientErrorException.NotFound ignore) {
            // ignore
        }
    }

    public String loadMappingBody() {
        if (!StringUtils.hasText(properties.getMappingResource())) {
            throw new IllegalStateException("缺少 search.es.mapping-resource 配置");
        }
        try {
            Resource resource = resourceLoader.getResource(properties.getMappingResource());
            byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("读取 ES mapping 资源失败", ex);
        }
    }

    private Map<String, Object> buildDocument(ProductSearchSnapshot snapshot) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        doc.put("product_id", snapshot.getProductId());
        doc.put("seller_id", snapshot.getSellerId());
        doc.put("store_id", snapshot.getStoreId());
        doc.put("store_name", snapshot.getStoreName());
        doc.put("product_type", snapshot.getProductType());
        doc.put("title", snapshot.getTitle());
        doc.put("subtitle", snapshot.getSubtitle());
        doc.put("summary_text", snapshot.getSummaryText());
        doc.put("category_id", snapshot.getCategoryId());
        doc.put("category_name", snapshot.getCategoryName());
        doc.put("category_path", snapshot.getCategoryPath());
        doc.put("tag_ids", splitJsonArray(snapshot.getTagIds()));
        doc.put("tag_names", splitJsonArray(snapshot.getTagNames()));
        doc.put("city_id", snapshot.getCityId());
        doc.put("city_name", snapshot.getCityName());
        doc.put("district_id", snapshot.getDistrictId());
        doc.put("district_name", snapshot.getDistrictName());
        doc.put("business_area_id", snapshot.getBusinessAreaId());
        doc.put("business_area_name", snapshot.getBusinessAreaName());
        doc.put("lat", snapshot.getLat());
        doc.put("lng", snapshot.getLng());
        doc.put("base_price", snapshot.getBasePrice());
        doc.put("display_price", snapshot.getDisplayPrice());
        doc.put("currency", snapshot.getCurrency());
        doc.put("cover_image", snapshot.getCoverImage());
        doc.put("sales_count", snapshot.getSalesCount());
        doc.put("rating", snapshot.getRating());
        doc.put("review_count", snapshot.getReviewCount());
        doc.put("hot_score", snapshot.getHotScore());
        doc.put("recommend_score", snapshot.getRecommendScore());
        doc.put("searchable_status", snapshot.getSearchableStatus());
        doc.put("publish_status", snapshot.getPublishStatus());
        doc.put("audit_status", snapshot.getAuditStatus());
        doc.put("visible_status", snapshot.getVisibleStatus());
        doc.put("source_status", snapshot.getSourceStatus());
        doc.put("updated_at", snapshot.getUpdatedAt());
        doc.put("searchable_updated_at", snapshot.getSearchableUpdatedAt());
        return doc;
    }

    private List<String> splitJsonArray(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String value = raw.trim();
        if (value.startsWith("[")) {
            try {
                return objectMapper.readValue(value, new TypeReference<List<String>>() {
                });
            } catch (IOException ignore) {
                // ignore
            }
        }
        String[] tokens = value.replace("[", "").replace("]", "").replace("\"", "").split(",");
        List<String> result = new ArrayList<String>();
        for (String token : tokens) {
            String normalized = token == null ? "" : token.trim();
            if (StringUtils.hasText(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String body) {
        return exchange(path, method, body, MediaType.APPLICATION_JSON_VALUE);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String body, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (StringUtils.hasText(properties.getUsername())) {
            headers.setBasicAuth(properties.getUsername(), properties.getPassword() == null ? "" : properties.getPassword(), StandardCharsets.UTF_8);
        }
        HttpEntity<String> entity = new HttpEntity<String>(body, headers);
        return restTemplate.exchange(normalizeUrl(path), method, entity, String.class);
    }

    private String normalizeUrl(String path) {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private Map<String, Object> readObject(String body) {
        if (!StringUtils.hasText(body)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("解析 ES 响应失败", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Object item : raw) {
            if (item instanceof Map) {
                results.add((Map<String, Object>) item);
            }
        }
        return results;
    }

    private String writeJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException ex) {
            throw new IllegalStateException("序列化 ES 请求失败", ex);
        }
    }
}

package com.example.demo.demos.search.es;

import com.example.demo.demos.search.config.SearchEsProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ProductSearchIndexManager {

    private final ProductSearchEsClient esClient;
    private final SearchEsProperties properties;

    public ProductSearchIndexManager(ProductSearchEsClient esClient, SearchEsProperties properties) {
        this.esClient = esClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return esClient.isEnabled();
    }

    public void ensureInitialized() {
        if (!isEnabled()) {
            return;
        }
        String indexName = getVersionedIndex();
        if (!esClient.indexExists(indexName)) {
            esClient.createIndex(indexName, esClient.loadMappingBody());
        }
        if (StringUtils.hasText(properties.getReadAlias()) && esClient.resolveAliasTargets(properties.getReadAlias()).isEmpty()) {
            esClient.swapAlias(properties.getReadAlias(), indexName);
        }
        if (StringUtils.hasText(properties.getWriteAlias()) && esClient.resolveAliasTargets(properties.getWriteAlias()).isEmpty()) {
            esClient.swapAlias(properties.getWriteAlias(), indexName);
        }
    }

    public String getVersionedIndex() {
        return properties.getVersionedIndex();
    }

    public boolean indexExists(String indexName) {
        return isEnabled() && StringUtils.hasText(indexName) && esClient.indexExists(indexName);
    }

    public String resolveWriteTarget() {
        List<String> targets = esClient.resolveAliasTargets(properties.getWriteAlias());
        return targets.isEmpty() ? getVersionedIndex() : targets.get(0);
    }

    public String resolveReadTarget() {
        List<String> targets = esClient.resolveAliasTargets(properties.getReadAlias());
        return targets.isEmpty() ? getVersionedIndex() : targets.get(0);
    }

    public Map<String, Object> validateAliases() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", isEnabled());
        if (!isEnabled()) {
            result.put("reachable", false);
            result.put("consistent", false);
            return result;
        }
        boolean reachable = esClient.ping();
        result.put("reachable", reachable);
        result.put("versionedIndex", getVersionedIndex());
        result.put("readAlias", properties.getReadAlias());
        result.put("writeAlias", properties.getWriteAlias());
        result.put("readTargets", esClient.resolveAliasTargets(properties.getReadAlias()));
        result.put("writeTargets", esClient.resolveAliasTargets(properties.getWriteAlias()));
        result.put("readTarget", resolveReadTarget());
        result.put("writeTarget", resolveWriteTarget());
        result.put("sameTarget", resolveReadTarget().equals(resolveWriteTarget()));
        result.put("readCount", reachable ? esClient.countDocuments(resolveReadTarget()) : 0L);
        result.put("writeCount", reachable ? esClient.countDocuments(resolveWriteTarget()) : 0L);
        result.put("consistent", reachable
                && !Collections.emptyList().equals(result.get("readTargets"))
                && !Collections.emptyList().equals(result.get("writeTargets"))
                && Boolean.TRUE.equals(result.get("sameTarget")));
        return result;
    }

    public void recreateIndex(String targetIndex) {
        if (!isEnabled()) {
            return;
        }
        esClient.deleteIndex(targetIndex);
        esClient.createIndex(targetIndex, esClient.loadMappingBody());
    }

    public void attachReadAlias(String targetIndex) {
        esClient.swapAlias(properties.getReadAlias(), targetIndex);
    }

    public void attachWriteAlias(String targetIndex) {
        esClient.swapAlias(properties.getWriteAlias(), targetIndex);
    }

    public Map<String, Object> switchAliases(String targetIndex) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("targetIndex", targetIndex);
        result.put("enabled", isEnabled());
        if (!isEnabled()) {
            result.put("switched", false);
            result.put("reason", "search.es.enabled=false");
            return result;
        }
        if (!StringUtils.hasText(targetIndex)) {
            result.put("switched", false);
            result.put("reason", "targetIndex is required");
            return result;
        }
        if (!indexExists(targetIndex)) {
            result.put("switched", false);
            result.put("reason", "targetIndex not found");
            return result;
        }
        attachReadAlias(targetIndex);
        attachWriteAlias(targetIndex);
        result.put("switched", true);
        result.put("validation", validateAliases());
        return result;
    }

    public List<Map<String, Object>> listRollbackCandidates() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (!isEnabled()) {
            return result;
        }
        String readTarget = resolveReadTarget();
        String writeTarget = resolveWriteTarget();
        Set<String> candidates = new LinkedHashSet<String>();
        if (StringUtils.hasText(readTarget)) {
            candidates.add(readTarget);
        }
        if (StringUtils.hasText(writeTarget)) {
            candidates.add(writeTarget);
        }
        if (StringUtils.hasText(getVersionedIndex())) {
            candidates.add(getVersionedIndex());
        }
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("index", candidate);
            item.put("exists", indexExists(candidate));
            item.put("currentRead", candidate.equals(readTarget));
            item.put("currentWrite", candidate.equals(writeTarget));
            item.put("documentCount", indexExists(candidate) ? esClient.countDocuments(candidate) : 0L);
            item.put("recommended", candidate.equals(getVersionedIndex()) && !candidate.equals(readTarget));
            result.add(item);
        }
        return result;
    }
}

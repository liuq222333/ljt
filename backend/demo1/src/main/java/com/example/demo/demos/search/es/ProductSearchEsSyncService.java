package com.example.demo.demos.search.es;

import com.example.demo.demos.search.config.SearchEsProperties;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProductSearchEsSyncService {

    private final ProductSearchEsClient esClient;
    private final ProductSearchIndexManager indexManager;
    private final ProductSearchSnapshotMapper snapshotMapper;
    private final SearchEsProperties properties;

    public ProductSearchEsSyncService(ProductSearchEsClient esClient,
                                      ProductSearchIndexManager indexManager,
                                      ProductSearchSnapshotMapper snapshotMapper,
                                      SearchEsProperties properties) {
        this.esClient = esClient;
        this.indexManager = indexManager;
        this.snapshotMapper = snapshotMapper;
        this.properties = properties;
    }

    public void upsertSnapshot(ProductSearchSnapshot snapshot) {
        if (snapshot == null || !esClient.isEnabled()) {
            return;
        }
        indexManager.ensureInitialized();
        if (!"searchable".equalsIgnoreCase(snapshot.getSearchableStatus())) {
            esClient.deleteDocument(indexManager.resolveWriteTarget(), snapshot.getProductId());
            return;
        }
        esClient.bulkUpsert(indexManager.resolveWriteTarget(), Collections.singletonList(snapshot));
    }

    public Map<String, Object> rebuildVersionedIndex(String targetIndex) {
        if (!esClient.isEnabled()) {
            return buildDisabledResult();
        }
        String index = targetIndex == null || targetIndex.trim().isEmpty()
                ? indexManager.getVersionedIndex()
                : targetIndex.trim();
        indexManager.recreateIndex(index);

        int batchSize = Math.max(properties.getSyncBatchSize(), 1);
        int offset = 0;
        long total = 0L;
        while (true) {
            List<ProductSearchSnapshot> page = snapshotMapper.selectSearchablePage(offset, batchSize);
            if (CollectionUtils.isEmpty(page)) {
                break;
            }
            esClient.bulkUpsert(index, page);
            total += page.size();
            offset += page.size();
            if (page.size() < batchSize) {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("targetIndex", index);
        result.put("indexedCount", total);
        result.put("rebuildAt", LocalDateTime.now());
        result.put("readAlias", indexManager.resolveReadTarget());
        result.put("writeAlias", indexManager.resolveWriteTarget());
        return result;
    }

    public Map<String, Object> repairDocumentsByIds(List<Long> productIds, String targetIndex) {
        if (CollectionUtils.isEmpty(productIds) || !esClient.isEnabled()) {
            return buildDisabledResult();
        }
        indexManager.ensureInitialized();
        String index = targetIndex == null || targetIndex.trim().isEmpty()
                ? indexManager.resolveWriteTarget()
                : targetIndex.trim();
        List<ProductSearchSnapshot> upserts = new ArrayList<ProductSearchSnapshot>();
        int deleted = 0;
        for (Long productId : productIds) {
            ProductSearchSnapshot snapshot = snapshotMapper.selectByProductId(productId);
            if (snapshot == null || !"searchable".equalsIgnoreCase(snapshot.getSearchableStatus())) {
                esClient.deleteDocument(index, productId);
                deleted++;
                continue;
            }
            upserts.add(snapshot);
        }
        if (!upserts.isEmpty()) {
            esClient.bulkUpsert(index, upserts);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("targetIndex", index);
        result.put("requestedCount", productIds.size());
        result.put("upsertedCount", upserts.size());
        result.put("deletedCount", deleted);
        result.put("requestedIds", productIds);
        return result;
    }

    public Map<String, Object> validateAgainstSnapshot(String indexOrAlias, int sampleLimit) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", esClient.isEnabled());
        result.put("indexOrAlias", indexOrAlias);
        if (!esClient.isEnabled()) {
            result.put("consistent", false);
            return result;
        }
        Set<Long> snapshotIds = new java.util.LinkedHashSet<Long>(snapshotMapper.selectSearchableProductIds());
        Set<Long> indexedIds = esClient.listIndexedProductIds(indexOrAlias, Math.max(sampleLimit, 2000));

        List<Long> missingIds = new ArrayList<Long>();
        for (Long snapshotId : snapshotIds) {
            if (!indexedIds.contains(snapshotId)) {
                missingIds.add(snapshotId);
            }
        }
        List<Long> staleIds = new ArrayList<Long>();
        for (Long indexedId : indexedIds) {
            if (!snapshotIds.contains(indexedId)) {
                staleIds.add(indexedId);
            }
        }
        Collections.sort(missingIds);
        Collections.sort(staleIds);
        result.put("snapshotCount", snapshotIds.size());
        result.put("indexCount", indexedIds.size());
        result.put("missingSnapshotIds", trim(missingIds, sampleLimit));
        result.put("staleIndexIds", trim(staleIds, sampleLimit));
        result.put("consistent", missingIds.isEmpty() && staleIds.isEmpty());
        return result;
    }

    private List<Long> trim(List<Long> values, int limit) {
        int safeLimit = Math.max(limit, 20);
        if (values.size() <= safeLimit) {
            return values;
        }
        return new ArrayList<Long>(values.subList(0, safeLimit));
    }

    private Map<String, Object> buildDisabledResult() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("enabled", false);
        result.put("message", "search.es.enabled=false");
        return result;
    }
}

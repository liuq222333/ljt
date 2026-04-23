package com.example.demo.demos.sync;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Service.KnowledgeVectorService;
import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.search.es.ProductSearchEsSyncService;
import com.example.demo.demos.search.es.ProductSearchIndexManager;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.snapshot.ProductSnapshotBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SyncAdminService {

    private final ProductsMapper productsMapper;
    private final ProductSnapshotBuilder productSnapshotBuilder;
    private final ProductSearchSnapshotMapper productSearchSnapshotMapper;
    private final ProductSearchEsSyncService productSearchEsSyncService;
    private final ProductSearchIndexManager productSearchIndexManager;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeVectorService knowledgeVectorService;
    private final SyncTaskTracker syncTaskTracker;

    public SyncAdminService(ProductsMapper productsMapper,
                            ProductSnapshotBuilder productSnapshotBuilder,
                            ProductSearchSnapshotMapper productSearchSnapshotMapper,
                            ProductSearchEsSyncService productSearchEsSyncService,
                            ProductSearchIndexManager productSearchIndexManager,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            KnowledgeVectorService knowledgeVectorService,
                            SyncTaskTracker syncTaskTracker) {
        this.productsMapper = productsMapper;
        this.productSnapshotBuilder = productSnapshotBuilder;
        this.productSearchSnapshotMapper = productSearchSnapshotMapper;
        this.productSearchEsSyncService = productSearchEsSyncService;
        this.productSearchIndexManager = productSearchIndexManager;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeVectorService = knowledgeVectorService;
        this.syncTaskTracker = syncTaskTracker;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("products", reconcileProducts());
        result.put("knowledge", reconcileKnowledge(null, null, null, null, 50));
        result.put("tasks", syncTaskTracker.summary());
        return result;
    }

    public Map<String, Object> rebuildProducts(String targetIndex) {
        long taskId = syncTaskTracker.start("rebuild_products", mapOf("targetIndex", targetIndex));
        try {
            List<Product> products = productsMapper.getAllProducts();
            int success = 0;
            int failed = 0;
            for (Product product : products) {
                try {
                    productSnapshotBuilder.buildAndSave(product);
                    success++;
                } catch (Exception ex) {
                    failed++;
                }
            }
            Map<String, Object> esResult = productSearchEsSyncService.rebuildVersionedIndex(targetIndex);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("productTotal", products.size());
            result.put("snapshotSuccess", success);
            result.put("snapshotFailed", failed);
            result.put("es", esResult);
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> rebuildProductsVersioned(String targetIndex, boolean switchAlias) {
        String normalizedTarget = StringUtils.hasText(targetIndex) ? targetIndex.trim() : productSearchIndexManager.getVersionedIndex();
        Map<String, Object> result = rebuildProducts(normalizedTarget);
        result.put("targetIndex", normalizedTarget);
        result.put("switchAliasRequested", switchAlias);
        if (switchAlias) {
            result.put("aliasSwitch", switchProductAliases(normalizedTarget));
        }
        result.put("validation", validateProductIndex());
        return result;
    }

    public Map<String, Object> repairProduct(Long productId) {
        if (productId == null) {
            return mapOf("repaired", false, "reason", "productId is required");
        }
        return repairProductsByIds(Collections.singletonList(productId), null);
    }

    public Map<String, Object> repairProductsByIds(List<Long> productIds, String targetIndex) {
        long taskId = syncTaskTracker.start("repair_products_by_ids", mapOf("productIds", productIds, "targetIndex", targetIndex));
        try {
            if (CollectionUtils.isEmpty(productIds)) {
                Map<String, Object> empty = mapOf("requestedCount", 0, "repairedCount", 0);
                syncTaskTracker.complete(taskId, empty);
                return empty;
            }
            List<Long> repairedIds = new ArrayList<Long>();
            List<Long> missingIds = new ArrayList<Long>();
            for (Long productId : productIds) {
                Product product = productsMapper.getProductById(productId);
                if (product == null) {
                    missingIds.add(productId);
                    productSearchSnapshotMapper.deleteByProductId(productId);
                    continue;
                }
                productSnapshotBuilder.buildAndSave(product);
                repairedIds.add(productId);
            }
            Map<String, Object> esResult = productSearchEsSyncService.repairDocumentsByIds(productIds, targetIndex);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("requestedCount", productIds.size());
            result.put("repairedCount", repairedIds.size());
            result.put("repairedIds", repairedIds);
            result.put("missingIds", missingIds);
            result.put("es", esResult);
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> repairProductsByUpdatedRange(LocalDateTime start, LocalDateTime end, Integer limit, String targetIndex) {
        List<Long> ids = productsMapper.selectProductIdsByUpdatedRange(start, end, normalizeLimit(limit));
        Map<String, Object> result = repairProductsByIds(ids, targetIndex);
        result.put("rangeStart", start);
        result.put("rangeEnd", end);
        return result;
    }

    public Map<String, Object> incrementalProducts(LocalDateTime start, LocalDateTime end, Integer limit, String targetIndex) {
        Map<String, Object> result = repairProductsByUpdatedRange(start, end, limit, targetIndex);
        result.put("mode", "incremental");
        return result;
    }

    public Map<String, Object> repairProductsByStatus(String status, Integer limit, String targetIndex) {
        List<Long> ids = productsMapper.selectProductIdsByStatus(status, normalizeLimit(limit));
        Map<String, Object> result = repairProductsByIds(ids, targetIndex);
        result.put("status", status);
        return result;
    }

    public Map<String, Object> repairProductsByCategory(Long categoryId, String status, Integer limit, String targetIndex) {
        List<Long> ids = productsMapper.selectProductIdsByCategoryId(categoryId, status, normalizeLimit(limit));
        Map<String, Object> result = repairProductsByIds(ids, targetIndex);
        result.put("categoryId", categoryId);
        result.put("status", status);
        return result;
    }

    public Map<String, Object> reconcileProducts() {
        List<Long> sourceIds = safeList(productsMapper.selectAllProductIds());
        List<Long> snapshotIds = safeList(productSearchSnapshotMapper.selectAllProductIds());
        Set<Long> sourceSet = new LinkedHashSet<Long>(sourceIds);
        Set<Long> snapshotSet = new LinkedHashSet<Long>(snapshotIds);

        List<Long> missingSnapshotIds = difference(sourceSet, snapshotSet);
        List<Long> staleSnapshotIds = difference(snapshotSet, sourceSet);

        Map<String, Object> esValidation = validateProductIndex();
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sourceCount", sourceIds.size());
        result.put("snapshotCount", snapshotIds.size());
        result.put("searchableSnapshotCount", productSearchSnapshotMapper.countSearchable());
        result.put("missingSnapshotIds", trim(missingSnapshotIds, 20));
        result.put("staleSnapshotIds", trim(staleSnapshotIds, 20));
        result.put("indexValidation", esValidation);
        result.put("consistent", missingSnapshotIds.isEmpty() && staleSnapshotIds.isEmpty() && Boolean.TRUE.equals(esValidation.get("consistent")));
        return result;
    }

    public Map<String, Object> reconcileProductsDetail(Integer limit) {
        int sampleLimit = Math.max(limit == null ? 20 : limit.intValue(), 1);
        List<Long> sourceIds = safeList(productsMapper.selectAllProductIds());
        List<Long> snapshotIds = safeList(productSearchSnapshotMapper.selectAllProductIds());
        Set<Long> sourceSet = new LinkedHashSet<Long>(sourceIds);
        Set<Long> snapshotSet = new LinkedHashSet<Long>(snapshotIds);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("missingSnapshotIds", trim(difference(sourceSet, snapshotSet), sampleLimit));
        result.put("staleSnapshotIds", trim(difference(snapshotSet, sourceSet), sampleLimit));
        result.put("indexValidation", productSearchEsSyncService.validateAgainstSnapshot(productSearchIndexManager.resolveReadTarget(), sampleLimit));
        return result;
    }

    public Map<String, Object> switchProductAliases(String targetIndex) {
        long taskId = syncTaskTracker.start("switch_product_aliases", mapOf("targetIndex", targetIndex));
        try {
            Map<String, Object> result = productSearchIndexManager.switchAliases(targetIndex);
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public List<Map<String, Object>> listProductRollbackCandidates() {
        return productSearchIndexManager.listRollbackCandidates();
    }

    public Map<String, Object> autoRollbackProducts() {
        List<Map<String, Object>> candidates = listProductRollbackCandidates();
        for (Map<String, Object> candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.get("recommended"))
                    && Boolean.TRUE.equals(candidate.get("exists"))
                    && longValue(candidate.get("documentCount")) > 0) {
                Map<String, Object> result = switchProductAliases(String.valueOf(candidate.get("index")));
                result.put("rollbackMode", "auto");
                return result;
            }
        }
        return mapOf(
                "rollbackMode", "auto",
                "switched", false,
                "reason", "no_valid_candidate"
        );
    }

    public Map<String, Object> validateProductIndex() {
        Map<String, Object> aliasValidation = productSearchIndexManager.validateAliases();
        if (!Boolean.TRUE.equals(aliasValidation.get("enabled")) || !Boolean.TRUE.equals(aliasValidation.get("reachable"))) {
            aliasValidation.put("snapshotValidation", Collections.emptyMap());
            aliasValidation.put("consistent", false);
            return aliasValidation;
        }
        Map<String, Object> snapshotValidation = productSearchEsSyncService.validateAgainstSnapshot(productSearchIndexManager.resolveReadTarget(), 20);
        aliasValidation.put("snapshotValidation", snapshotValidation);
        aliasValidation.put("consistent", Boolean.TRUE.equals(aliasValidation.get("consistent")) && Boolean.TRUE.equals(snapshotValidation.get("consistent")));
        return aliasValidation;
    }

    public Map<String, Object> rebuildKnowledge(boolean rebuildExisting) {
        long taskId = syncTaskTracker.start("rebuild_knowledge_vectors", mapOf("rebuildExisting", rebuildExisting));
        try {
            int generated = knowledgeVectorService.generateVectors(rebuildExisting);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("generated", generated);
            result.put("stats", knowledgeVectorService.collectVectorStats());
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> repairKnowledge(Long knowledgeId) {
        long taskId = syncTaskTracker.start("repair_knowledge", mapOf("knowledgeId", knowledgeId));
        try {
            int repaired = knowledgeVectorService.rebuildVector(knowledgeId);
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("knowledgeId", knowledgeId);
            result.put("repaired", repaired > 0);
            result.put("stats", knowledgeVectorService.collectVectorStats());
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> repairKnowledgeByIds(List<Long> knowledgeIds) {
        long taskId = syncTaskTracker.start("repair_knowledge_by_ids", mapOf("knowledgeIds", knowledgeIds));
        try {
            int repaired = 0;
            List<Long> repairedIds = new ArrayList<Long>();
            for (Long knowledgeId : knowledgeIds) {
                if (knowledgeVectorService.rebuildVector(knowledgeId) > 0) {
                    repaired++;
                    repairedIds.add(knowledgeId);
                }
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("requestedCount", knowledgeIds.size());
            result.put("repairedCount", repaired);
            result.put("repairedIds", repairedIds);
            result.put("stats", knowledgeVectorService.collectVectorStats());
            syncTaskTracker.complete(taskId, result);
            return result;
        } catch (Exception ex) {
            syncTaskTracker.fail(taskId, ex.getMessage());
            throw ex;
        }
    }

    public Map<String, Object> repairKnowledgeByFilters(String docType,
                                                        String entityType,
                                                        String entityId,
                                                        Integer status,
                                                        Integer limit) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq("entity_type", entityType);
        }
        if (StringUtils.hasText(entityId)) {
            wrapper.eq("entity_id", entityId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("updated_at").orderByAsc("id");
        int safeLimit = normalizeLimit(limit);
        if (safeLimit > 0) {
            wrapper.last("LIMIT " + safeLimit);
        }
        List<KnowledgeBase> rows = knowledgeBaseMapper.selectList(wrapper);
        List<Long> ids = new ArrayList<Long>();
        for (KnowledgeBase row : rows) {
            if (row != null && row.getId() != null) {
                ids.add(row.getId());
            }
        }
        Map<String, Object> result = repairKnowledgeByIds(ids);
        result.put("docType", docType);
        result.put("entityType", entityType);
        result.put("entityId", entityId);
        result.put("status", status);
        return result;
    }

    public Map<String, Object> reconcileKnowledge(String docType,
                                                  String entityType,
                                                  String entityId,
                                                  Integer status,
                                                  Integer limit) {
        if (StringUtils.hasText(docType) || StringUtils.hasText(entityType) || StringUtils.hasText(entityId) || status != null || limit != null) {
            return knowledgeVectorService.collectVectorStats(docType, entityType, entityId, status, normalizeLimit(limit));
        }
        return knowledgeVectorService.collectVectorStats();
    }

    public Map<String, Object> incrementalKnowledge(LocalDateTime start,
                                                    LocalDateTime end,
                                                    String docType,
                                                    String entityType,
                                                    String entityId,
                                                    Integer status,
                                                    Integer limit) {
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<KnowledgeBase>();
        if (start != null) {
            wrapper.ge("updated_at", start);
        }
        if (end != null) {
            wrapper.le("updated_at", end);
        }
        if (StringUtils.hasText(docType)) {
            wrapper.eq("doc_type", docType);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq("entity_type", entityType);
        }
        if (StringUtils.hasText(entityId)) {
            wrapper.eq("entity_id", entityId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("updated_at").orderByAsc("id");
        int safeLimit = normalizeLimit(limit);
        wrapper.last("LIMIT " + safeLimit);
        List<KnowledgeBase> rows = knowledgeBaseMapper.selectList(wrapper);
        List<Long> ids = new ArrayList<Long>();
        for (KnowledgeBase row : rows) {
            if (row != null && row.getId() != null) {
                ids.add(row.getId());
            }
        }
        Map<String, Object> result = repairKnowledgeByIds(ids);
        result.put("mode", "incremental");
        result.put("rangeStart", start);
        result.put("rangeEnd", end);
        result.put("docType", docType);
        result.put("entityType", entityType);
        result.put("entityId", entityId);
        result.put("status", status);
        return result;
    }

    public Map<String, Object> baselineReport() {
        Map<String, Object> products = reconcileProducts();
        Map<String, Object> knowledge = reconcileKnowledge(null, null, null, null, 50);
        Map<String, Object> tasks = syncTaskTracker.summary();
        List<Map<String, Object>> repairRecommendations = buildRepairRecommendations(products, knowledge, tasks);
        boolean ready = Boolean.TRUE.equals(products.get("consistent"))
                && Boolean.TRUE.equals(knowledge.get("consistent"))
                && intValue(tasks.get("failed")) == 0
                && intValue(tasks.get("deadLetterTotal")) == 0;
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("generatedAt", LocalDateTime.now());
        result.put("products", products);
        result.put("knowledge", knowledge);
        result.put("tasks", tasks);
        result.put("repairRecommendations", repairRecommendations);
        result.put("w13Bridge", mapOf(
                "canSeedReplay", !repairRecommendations.isEmpty(),
                "candidateMismatchCount", sizeOf(products.get("missingSnapshotIds")) + sizeOf(products.get("staleSnapshotIds")),
                "knowledgeMismatchCount", intValue(knowledge.get("missing_active_vector_total")) + intValue(knowledge.get("stale_vector_total"))
        ));
        result.put("w12Ready", ready);
        return result;
    }

    public Map<String, Object> repairStrategyReport() {
        Map<String, Object> products = reconcileProducts();
        Map<String, Object> knowledge = reconcileKnowledge(null, null, null, null, 50);
        Map<String, Object> tasks = syncTaskTracker.summary();
        return mapOf(
                "products", products,
                "knowledge", knowledge,
                "tasks", tasks,
                "recommendations", buildRepairRecommendations(products, knowledge, tasks)
        );
    }

    public Map<String, Object> retryTask(long taskId) {
        Map<String, Object> task = getTask(taskId);
        if (task.isEmpty()) {
            return mapOf("retried", false, "reason", "task_not_found", "taskId", taskId);
        }
        Map<String, Object> payload = asMap(task.get("payload"));
        String taskType = String.valueOf(task.get("taskType"));
        if ("rebuild_products".equals(taskType)) {
            Map<String, Object> result = rebuildProducts(asString(payload.get("targetIndex")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        if ("repair_products_by_ids".equals(taskType)) {
            Map<String, Object> result = repairProductsByIds(longList(payload.get("productIds")), asString(payload.get("targetIndex")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        if ("switch_product_aliases".equals(taskType)) {
            Map<String, Object> result = switchProductAliases(asString(payload.get("targetIndex")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        if ("rebuild_knowledge_vectors".equals(taskType)) {
            Map<String, Object> result = rebuildKnowledge(Boolean.TRUE.equals(payload.get("rebuildExisting")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        if ("repair_knowledge".equals(taskType)) {
            Map<String, Object> result = repairKnowledge(longValue(payload.get("knowledgeId")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        if ("repair_knowledge_by_ids".equals(taskType)) {
            Map<String, Object> result = repairKnowledgeByIds(longList(payload.get("knowledgeIds")));
            result.put("retried", true);
            result.put("retryOf", taskId);
            return result;
        }
        return mapOf("retried", false, "reason", "task_retry_not_supported", "taskId", taskId, "taskType", taskType);
    }

    public List<Map<String, Object>> listTasks() {
        return syncTaskTracker.listTasks();
    }

    public Map<String, Object> getTask(long taskId) {
        return syncTaskTracker.getTask(taskId);
    }

    public List<Map<String, Object>> deadLetters() {
        return syncTaskTracker.listDeadLetters();
    }

    private List<Map<String, Object>> buildRepairRecommendations(Map<String, Object> products,
                                                                 Map<String, Object> knowledge,
                                                                 Map<String, Object> tasks) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (!Boolean.TRUE.equals(products.get("consistent"))) {
            result.add(mapOf(
                    "type", "products_reconcile",
                    "severity", "high",
                    "action", "/api/sync/admin/reconcile/products/detail",
                    "reason", "product snapshot/index mismatch detected"
            ));
        }
        if (!Boolean.TRUE.equals(knowledge.get("consistent"))) {
            result.add(mapOf(
                    "type", "knowledge_reconcile",
                    "severity", "high",
                    "action", "/api/sync/admin/rebuild/knowledge",
                    "reason", "knowledge vector mismatch detected"
            ));
        }
        if (intValue(tasks.get("failed")) > 0 || intValue(tasks.get("deadLetterTotal")) > 0) {
            result.add(mapOf(
                    "type", "task_recovery",
                    "severity", "medium",
                    "action", "/api/sync/admin/dead-letters",
                    "reason", "failed sync tasks require retry or inspection"
            ));
        }
        Map<String, Object> indexValidation = asMap(products.get("indexValidation"));
        if (!Boolean.TRUE.equals(indexValidation.get("consistent"))) {
            result.add(mapOf(
                    "type", "index_alias_validation",
                    "severity", "high",
                    "action", "/api/sync/admin/products/index/validate",
                    "reason", "product read/write alias validation failed"
            ));
        }
        return result;
    }

    private List<Long> difference(Set<Long> left, Set<Long> right) {
        List<Long> result = new ArrayList<Long>();
        for (Long value : left) {
            if (!right.contains(value)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }

    private List<Long> trim(List<Long> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        return new ArrayList<Long>(values.subList(0, limit));
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return 200;
        }
        return Math.min(limit.intValue(), 1000);
    }

    private List<Long> safeList(List<Long> ids) {
        return ids == null ? Collections.<Long>emptyList() : ids;
    }

    private int intValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private long longValue(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Long> longList(Object raw) {
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<Object> values = (List<Object>) raw;
        List<Long> result = new ArrayList<Long>();
        for (Object value : values) {
            if (value instanceof Number) {
                result.add(((Number) value).longValue());
            } else if (value instanceof String && StringUtils.hasText((String) value)) {
                result.add(Long.valueOf((String) value));
            }
        }
        return result;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : null;
    }

    private int sizeOf(Object value) {
        return value instanceof List ? ((List<?>) value).size() : 0;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}

package com.example.demo.demos.sync;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.demos.Agent.Dao.KnowledgeBaseMapper;
import com.example.demo.demos.Agent.Entity.KnowledgeBase;
import com.example.demo.demos.Agent.Service.KnowledgeVectorService;
import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.search.es.ProductSearchEsSyncService;
import com.example.demo.demos.search.es.ProductSearchIndexManager;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.snapshot.ProductSnapshotBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncAdminServiceTest {

    @Mock
    private ProductsMapper productsMapper;
    @Mock
    private ProductSnapshotBuilder productSnapshotBuilder;
    @Mock
    private ProductSearchSnapshotMapper productSearchSnapshotMapper;
    @Mock
    private ProductSearchEsSyncService productSearchEsSyncService;
    @Mock
    private ProductSearchIndexManager productSearchIndexManager;
    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock
    private KnowledgeVectorService knowledgeVectorService;

    private SyncTaskTracker syncTaskTracker;
    private SyncAdminService service;

    @BeforeEach
    void setUp() {
        syncTaskTracker = new SyncTaskTracker();
        service = new SyncAdminService(
                productsMapper,
                productSnapshotBuilder,
                productSearchSnapshotMapper,
                productSearchEsSyncService,
                productSearchIndexManager,
                knowledgeBaseMapper,
                knowledgeVectorService,
                syncTaskTracker
        );
    }

    @Test
    void reconcileProductsShouldExposeMissingAndStaleIds() {
        when(productsMapper.selectAllProductIds()).thenReturn(Arrays.asList(1L, 2L, 3L));
        when(productSearchSnapshotMapper.selectAllProductIds()).thenReturn(Arrays.asList(2L, 3L, 4L));
        when(productSearchSnapshotMapper.countSearchable()).thenReturn(2L);
        when(productSearchIndexManager.resolveReadTarget()).thenReturn("product_search_read");
        when(productSearchIndexManager.validateAliases()).thenReturn(mapOf("enabled", true, "reachable", true, "consistent", true));
        doReturn(mapOf("consistent", true, "missingSnapshotIds", Collections.emptyList(), "staleIndexIds", Collections.emptyList()))
                .when(productSearchEsSyncService)
                .validateAgainstSnapshot("product_search_read", 20);

        Map<String, Object> result = service.reconcileProducts();

        assertEquals(3, ((Number) result.get("sourceCount")).intValue());
        assertEquals(Collections.singletonList(1L), result.get("missingSnapshotIds"));
        assertEquals(Collections.singletonList(4L), result.get("staleSnapshotIds"));
        assertFalse((Boolean) result.get("consistent"));
    }

    @Test
    void repairProductsByIdsShouldRebuildExistingAndDeleteMissing() {
        Product product = new Product();
        product.setId(10L);
        product.setTitle("test");

        when(productsMapper.getProductById(10L)).thenReturn(product);
        when(productsMapper.getProductById(11L)).thenReturn(null);
        when(productSearchEsSyncService.repairDocumentsByIds(anyList(), anyString()))
                .thenReturn(mapOf("upsertedCount", 1, "deletedCount", 1));

        Map<String, Object> result = service.repairProductsByIds(Arrays.asList(10L, 11L), "product_search_v1");

        verify(productSnapshotBuilder).buildAndSave(product);
        verify(productSearchSnapshotMapper).deleteByProductId(11L);
        assertEquals(2, ((Number) result.get("requestedCount")).intValue());
        assertEquals(Collections.singletonList(10L), result.get("repairedIds"));
        assertEquals(Collections.singletonList(11L), result.get("missingIds"));
    }

    @Test
    void rebuildKnowledgeShouldReturnVectorStats() {
        when(knowledgeVectorService.generateVectors(true)).thenReturn(6);
        when(knowledgeVectorService.collectVectorStats()).thenReturn(mapOf("consistent", true, "vector_total", 6));

        Map<String, Object> result = service.rebuildKnowledge(true);

        assertEquals(6, ((Number) result.get("generated")).intValue());
        assertEquals(Boolean.TRUE, asMap(result.get("stats")).get("consistent"));
        assertEquals(1, syncTaskTracker.listTasks().size());
    }

    @Test
    void baselineReportShouldBeFalseWhenTasksFailed() {
        when(productsMapper.selectAllProductIds()).thenReturn(Collections.singletonList(1L));
        when(productSearchSnapshotMapper.selectAllProductIds()).thenReturn(Collections.singletonList(1L));
        when(productSearchSnapshotMapper.countSearchable()).thenReturn(1L);
        when(productSearchIndexManager.resolveReadTarget()).thenReturn("product_search_read");
        when(productSearchIndexManager.validateAliases()).thenReturn(mapOf("enabled", true, "reachable", true, "consistent", true));
        doReturn(mapOf("consistent", true, "missingSnapshotIds", Collections.emptyList(), "staleIndexIds", Collections.emptyList()))
                .when(productSearchEsSyncService)
                .validateAgainstSnapshot("product_search_read", 20);
        when(knowledgeVectorService.collectVectorStats(null, null, null, null, 50)).thenReturn(mapOf("consistent", true));

        long taskId = syncTaskTracker.start("manual_failed_task", mapOf("scope", "test"));
        syncTaskTracker.fail(taskId, "boom");

        Map<String, Object> report = service.baselineReport();

        assertEquals(Boolean.FALSE, report.get("w12Ready"));
        assertEquals(1, ((Number) asMap(report.get("tasks")).get("failed")).intValue());
        assertFalse(((List<?>) report.get("repairRecommendations")).isEmpty());
        assertEquals(Boolean.TRUE, asMap(report.get("w13Bridge")).get("canSeedReplay"));
    }

    @Test
    void autoRollbackProductsShouldUseRecommendedCandidate() {
        when(productSearchIndexManager.listRollbackCandidates()).thenReturn(Collections.singletonList(
                mapOf("index", "product_search_v2", "exists", true, "recommended", true, "documentCount", 12L)
        ));
        when(productSearchIndexManager.switchAliases("product_search_v2"))
                .thenReturn(mapOf("switched", true, "validation", mapOf("consistent", true)));

        Map<String, Object> result = service.autoRollbackProducts();

        assertEquals("auto", result.get("rollbackMode"));
        assertEquals(Boolean.TRUE, result.get("switched"));
        verify(productSearchIndexManager).switchAliases("product_search_v2");
    }

    @Test
    void retryTaskShouldReplayProductRepair() {
        Product product = new Product();
        product.setId(10L);
        product.setTitle("retry-product");
        when(productsMapper.getProductById(10L)).thenReturn(product);
        when(productSearchEsSyncService.repairDocumentsByIds(anyList(), anyString()))
                .thenReturn(mapOf("upsertedCount", 1, "deletedCount", 0));

        long taskId = syncTaskTracker.start("repair_products_by_ids", mapOf(
                "productIds", Collections.singletonList(10L),
                "targetIndex", "product_search_v1"
        ));

        Map<String, Object> result = service.retryTask(taskId);

        assertEquals(Boolean.TRUE, result.get("retried"));
        assertEquals(taskId, ((Number) result.get("retryOf")).longValue());
        verify(productSnapshotBuilder).buildAndSave(product);
    }

    @Test
    void incrementalKnowledgeShouldRepairFilteredKnowledgeIds() {
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setId(8L);
        when(knowledgeBaseMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.singletonList(knowledge));
        when(knowledgeVectorService.rebuildVector(8L)).thenReturn(1);
        when(knowledgeVectorService.collectVectorStats()).thenReturn(mapOf("consistent", true));

        Map<String, Object> result = service.incrementalKnowledge(
                LocalDateTime.parse("2026-04-05T00:00:00"),
                LocalDateTime.parse("2026-04-05T23:59:59"),
                "faq",
                "product",
                "8",
                1,
                20
        );

        assertEquals("incremental", result.get("mode"));
        assertEquals(1, ((Number) result.get("repairedCount")).intValue());
        assertEquals(Collections.singletonList(8L), result.get("repairedIds"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}

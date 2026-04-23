package com.example.demo.demos.search.es;

import com.example.demo.demos.search.config.SearchEsProperties;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchEsSyncServiceTest {

    @Mock
    private ProductSearchEsClient esClient;
    @Mock
    private ProductSearchIndexManager indexManager;
    @Mock
    private ProductSearchSnapshotMapper snapshotMapper;

    private SearchEsProperties properties;
    private ProductSearchEsSyncService service;

    @BeforeEach
    void setUp() {
        properties = new SearchEsProperties();
        properties.setEnabled(true);
        properties.setSyncBatchSize(2);
        service = new ProductSearchEsSyncService(esClient, indexManager, snapshotMapper, properties);
    }

    @Test
    void upsertSnapshotShouldDeleteNonSearchableDocument() {
        ProductSearchSnapshot snapshot = snapshot(10L, "not_searchable");
        when(esClient.isEnabled()).thenReturn(true);
        when(indexManager.resolveWriteTarget()).thenReturn("product_search_write");

        service.upsertSnapshot(snapshot);

        verify(indexManager).ensureInitialized();
        verify(esClient).deleteDocument("product_search_write", 10L);
        verify(esClient, never()).bulkUpsert(eq("product_search_write"), anyList());
    }

    @Test
    void rebuildVersionedIndexShouldPageThroughSearchableSnapshots() {
        when(esClient.isEnabled()).thenReturn(true);
        when(indexManager.getVersionedIndex()).thenReturn("product_search_v1");
        when(snapshotMapper.selectSearchablePage(0, 2)).thenReturn(Arrays.asList(snapshot(1L, "searchable"), snapshot(2L, "searchable")));
        when(snapshotMapper.selectSearchablePage(2, 2)).thenReturn(Collections.singletonList(snapshot(3L, "searchable")));

        Map<String, Object> result = service.rebuildVersionedIndex(null);

        verify(indexManager).recreateIndex("product_search_v1");
        verify(esClient).bulkUpsert(eq("product_search_v1"), argThat(items -> items != null && items.size() == 2));
        verify(esClient).bulkUpsert(eq("product_search_v1"), argThat(items -> items != null && items.size() == 1));
        assertEquals(3L, ((Number) result.get("indexedCount")).longValue());
    }

    @Test
    void validateAgainstSnapshotShouldReturnMissingAndStaleIds() {
        when(esClient.isEnabled()).thenReturn(true);
        when(snapshotMapper.selectSearchableProductIds()).thenReturn(Arrays.asList(1L, 2L, 3L));
        when(esClient.listIndexedProductIds("product_search_read", 2000)).thenReturn(new java.util.LinkedHashSet<Long>(Arrays.asList(2L, 3L, 4L)));

        Map<String, Object> result = service.validateAgainstSnapshot("product_search_read", 50);

        assertEquals(Collections.singletonList(1L), result.get("missingSnapshotIds"));
        assertEquals(Collections.singletonList(4L), result.get("staleIndexIds"));
        assertEquals(Boolean.FALSE, result.get("consistent"));
    }

    private ProductSearchSnapshot snapshot(Long productId, String searchableStatus) {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(productId);
        snapshot.setSearchableStatus(searchableStatus);
        return snapshot;
    }
}

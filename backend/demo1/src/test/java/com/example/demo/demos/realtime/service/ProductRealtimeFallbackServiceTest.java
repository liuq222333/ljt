package com.example.demo.demos.realtime.service;

import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRealtimeFallbackServiceTest {

    @Mock
    private ProductsMapper productsMapper;
    @Mock
    private ProductSearchSnapshotMapper productSearchSnapshotMapper;

    private ProductRealtimeFallbackService service;

    @BeforeEach
    void setUp() {
        RealtimeQueryProperties properties = new RealtimeQueryProperties();
        properties.setFallbackSource("snapshot_fallback");
        properties.setDefaultCurrency("CNY");
        service = new ProductRealtimeFallbackService(productsMapper, productSearchSnapshotMapper, properties);
    }

    @Test
    void queryShouldBuildDegradedProductResultFromProductAndSnapshot() {
        Product product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("19.90"));
        product.setStockQuantity(3);
        product.setStatus("在售");
        when(productsMapper.selectByIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(product));

        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();
        snapshot.setProductId(1L);
        snapshot.setCurrency("CNY");
        when(productSearchSnapshotMapper.selectByProductIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(snapshot));

        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Collections.singletonList(1L));
        request.setQueryType("inventory");

        RealtimeQueryResponse response = service.query(request, request.getEntityIds(), "gateway_disabled");

        assertEquals(RealtimeStatus.DEGRADED, response.getRealtimeStatus());
        assertEquals(1, response.getItems().size());
        assertEquals("1", response.getItems().get(0).getEntityId());
        assertEquals("low_stock", response.getItems().get(0).getInventoryStatus());
        assertEquals("on_sale", response.getItems().get(0).getSellStatus());
        assertEquals(new BigDecimal("19.90"), response.getItems().get(0).getPrice());
        assertTrue(response.getItems().get(0).isDegraded());
    }

    @Test
    void queryProviderShouldReturnSuccessWithoutCallingOrchestrator() {
        Product product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("19.90"));
        product.setStockQuantity(8);
        product.setStatus("在售");
        when(productsMapper.selectByIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(product));
        when(productSearchSnapshotMapper.selectByProductIds(Collections.singletonList(1L))).thenReturn(Collections.<ProductSearchSnapshot>emptyList());

        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType("product");
        request.setEntityIds(Collections.singletonList(1L));
        request.setQueryType("inventory");

        RealtimeQueryResponse response = service.queryProvider(request, request.getEntityIds(), "internal_product_provider");

        assertEquals(RealtimeStatus.SUCCESS, response.getRealtimeStatus());
        assertEquals(Boolean.FALSE, response.getQueryMeta().get("fallbackUsed"));
        assertEquals("internal_product_provider", response.getItems().get(0).getSource());
        assertEquals(false, response.getItems().get(0).isDegraded());
    }
}

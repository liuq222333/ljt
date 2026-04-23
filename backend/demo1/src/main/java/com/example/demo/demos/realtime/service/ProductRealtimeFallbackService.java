package com.example.demo.demos.realtime.service;

import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.config.RealtimeQueryProperties;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeResultItem;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ProductRealtimeFallbackService {

    private final ProductsMapper productsMapper;
    private final ProductSearchSnapshotMapper productSearchSnapshotMapper;
    private final RealtimeQueryProperties properties;

    public ProductRealtimeFallbackService(ProductsMapper productsMapper,
                                          ProductSearchSnapshotMapper productSearchSnapshotMapper,
                                          RealtimeQueryProperties properties) {
        this.productsMapper = productsMapper;
        this.productSearchSnapshotMapper = productSearchSnapshotMapper;
        this.properties = properties;
    }

    public RealtimeQueryResponse query(RealtimeQueryRequest request, List<Long> targetIds, String fallbackReason) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        if (!"product".equalsIgnoreCase(request.getEntityType())) {
            response.setRealtimeStatus(RealtimeStatus.FAILED);
            response.getQueryMeta().put("fallbackUsed", true);
            response.getQueryMeta().put("fallbackReason", fallbackReason);
            response.getQueryMeta().put("source", properties.getFallbackSource());
            response.getQueryMeta().put("unsupportedEntityType", request.getEntityType());
            response.setPartialFailedIds(new ArrayList<Long>(targetIds));
            return response;
        }
        List<Long> failed = new ArrayList<Long>();
        for (Long entityId : targetIds) {
            RealtimeResultItem item = buildProductItem(entityId);
            if (item == null) {
                failed.add(entityId);
                continue;
            }
            response.getItems().add(item);
        }
        response.setPartialFailedIds(failed);
        if (response.getItems().isEmpty()) {
            response.setRealtimeStatus(RealtimeStatus.FAILED);
        } else if (!failed.isEmpty()) {
            response.setRealtimeStatus(RealtimeStatus.PARTIAL_SUCCESS);
        } else {
            response.setRealtimeStatus(RealtimeStatus.DEGRADED);
        }
        response.getQueryMeta().put("fallbackUsed", true);
        response.getQueryMeta().put("fallbackReason", fallbackReason);
        response.getQueryMeta().put("source", properties.getFallbackSource());
        response.getQueryMeta().put("queryCount", targetIds.size());
        return response;
    }

    private RealtimeResultItem buildProductItem(Long entityId) {
        Product product = productsMapper.getProductById(entityId);
        ProductSearchSnapshot snapshot = productSearchSnapshotMapper.selectByProductId(entityId);
        if (product == null && snapshot == null) {
            return null;
        }
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(String.valueOf(entityId));
        item.setQueryTs(LocalDateTime.now());
        item.setSuccess(true);
        item.setDegraded(true);
        item.setSource(properties.getFallbackSource());

        int stock = product != null && product.getStockQuantity() != null ? product.getStockQuantity().intValue() : 0;
        item.setInventoryCount(stock);
        item.setInventoryStatus(resolveInventoryStatus(stock, product == null ? null : product.getStatus()));
        item.setSellStatus(resolveSellStatus(product == null ? null : product.getStatus(), stock));
        item.setAvailabilityStatus(stock > 0 ? "available" : "sold_out");
        item.setBookable(stock > 0);
        item.setPrice(resolvePrice(product, snapshot));
        item.setCurrency(snapshot != null && snapshot.getCurrency() != null ? snapshot.getCurrency() : properties.getDefaultCurrency());
        item.setBusinessStatus("unknown");
        item.setOpenNow(null);
        return item;
    }

    private String resolveInventoryStatus(int stock, String status) {
        if ("下架".equals(status) || "off_shelf".equalsIgnoreCase(status)) {
            return "off_shelf";
        }
        if (stock <= 0) {
            return "sold_out";
        }
        if (stock <= 5) {
            return "low_stock";
        }
        return "available";
    }

    private String resolveSellStatus(String status, int stock) {
        if ("下架".equals(status) || "off_shelf".equalsIgnoreCase(status)) {
            return "off_shelf";
        }
        if (stock <= 0) {
            return "sold_out";
        }
        return "on_sale";
    }

    private BigDecimal resolvePrice(Product product, ProductSearchSnapshot snapshot) {
        if (product != null && product.getPrice() != null) {
            return product.getPrice();
        }
        if (snapshot != null && snapshot.getDisplayPrice() != null) {
            return snapshot.getDisplayPrice();
        }
        if (snapshot != null && snapshot.getBasePrice() != null) {
            return snapshot.getBasePrice();
        }
        return null;
    }
}

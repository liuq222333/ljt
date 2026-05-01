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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductRealtimeFallbackService implements RealtimeFallbackProvider {

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

    @Override
    public String getEntityType() {
        return "product";
    }

    @Override
    public RealtimeQueryResponse query(RealtimeQueryRequest request, List<Long> targetIds, String fallbackReason) {
        if (!"product".equalsIgnoreCase(request.getEntityType())) {
            RealtimeQueryResponse response = new RealtimeQueryResponse();
            response.setRealtimeStatus(RealtimeStatus.FAILED);
            response.getQueryMeta().put("fallbackUsed", true);
            response.getQueryMeta().put("fallbackReason", fallbackReason);
            response.getQueryMeta().put("source", properties.getFallbackSource());
            response.getQueryMeta().put("unsupportedEntityType", request.getEntityType());
            response.setPartialFailedIds(targetIds == null
                    ? new ArrayList<Long>()
                    : new ArrayList<Long>(targetIds));
            return response;
        }
        return buildProductResponse(
                request,
                targetIds,
                properties.getFallbackSource(),
                true,
                fallbackReason,
                true,
                RealtimeStatus.DEGRADED
        );
    }

    public RealtimeQueryResponse queryProvider(RealtimeQueryRequest request, List<Long> targetIds, String source) {
        return buildProductResponse(
                request,
                targetIds,
                source,
                false,
                "internal_product_provider",
                false,
                RealtimeStatus.SUCCESS
        );
    }

    private RealtimeQueryResponse buildProductResponse(RealtimeQueryRequest request,
                                                       List<Long> targetIds,
                                                       String source,
                                                       boolean fallbackUsed,
                                                       String reason,
                                                       boolean itemDegraded,
                                                       RealtimeStatus fullStatus) {
        RealtimeQueryResponse response = new RealtimeQueryResponse();
        List<Long> safeTargetIds = targetIds == null ? Collections.<Long>emptyList() : targetIds;
        Map<Long, Product> products = loadProducts(safeTargetIds);
        Map<Long, ProductSearchSnapshot> snapshots = loadSnapshots(safeTargetIds);
        List<Long> failed = new ArrayList<Long>();
        for (Long entityId : safeTargetIds) {
            RealtimeResultItem item = buildProductItem(
                    entityId,
                    products.get(entityId),
                    snapshots.get(entityId),
                    itemDegraded,
                    source
            );
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
            response.setRealtimeStatus(fullStatus);
        }
        response.getQueryMeta().put("fallbackUsed", fallbackUsed);
        response.getQueryMeta().put("fallbackReason", reason);
        response.getQueryMeta().put("source", source);
        response.getQueryMeta().put("queryCount", safeTargetIds.size());
        return response;
    }

    private Map<Long, Product> loadProducts(List<Long> targetIds) {
        Map<Long, Product> result = new LinkedHashMap<Long, Product>();
        if (targetIds.isEmpty()) {
            return result;
        }
        List<Product> products = productsMapper.selectByIds(targetIds);
        if (products == null) {
            return result;
        }
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                result.put(product.getId(), product);
            }
        }
        return result;
    }

    private Map<Long, ProductSearchSnapshot> loadSnapshots(List<Long> targetIds) {
        Map<Long, ProductSearchSnapshot> result = new LinkedHashMap<Long, ProductSearchSnapshot>();
        if (targetIds.isEmpty()) {
            return result;
        }
        List<ProductSearchSnapshot> snapshots = productSearchSnapshotMapper.selectByProductIds(targetIds);
        if (snapshots == null) {
            return result;
        }
        for (ProductSearchSnapshot snapshot : snapshots) {
            if (snapshot != null && snapshot.getProductId() != null) {
                result.put(snapshot.getProductId(), snapshot);
            }
        }
        return result;
    }

    private RealtimeResultItem buildProductItem(Long entityId,
                                                Product product,
                                                ProductSearchSnapshot snapshot,
                                                boolean degraded,
                                                String source) {
        if (product == null && snapshot == null) {
            return null;
        }
        RealtimeResultItem item = new RealtimeResultItem();
        item.setEntityId(String.valueOf(entityId));
        item.setQueryTs(LocalDateTime.now());
        item.setSuccess(true);
        item.setDegraded(degraded);
        item.setSource(source);

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

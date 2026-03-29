package com.example.demo.demos.search.snapshot;

import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.common.enums.SearchableStatus;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.entity.SearchCategory;
import com.example.demo.demos.search.es.ProductSearchEsSyncService;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.mapper.SearchCategoryMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Component
public class ProductSnapshotBuilder {

    private static final Logger log = LoggerFactory.getLogger(ProductSnapshotBuilder.class);

    @Autowired
    private SearchCategoryMapper searchCategoryMapper;

    @Autowired
    private ProductSearchSnapshotMapper productSearchSnapshotMapper;

    @Autowired
    private SearchableStatusCalculator statusCalculator;

    @Autowired
    private ObjectProvider<ProductSearchEsSyncService> productSearchEsSyncServiceProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductSearchSnapshot buildFromProduct(Product product) {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();

        snapshot.setProductId(product.getId());
        snapshot.setSellerId(product.getSellerId() != null ? product.getSellerId().longValue() : 0L);
        snapshot.setTitle(product.getTitle());
        snapshot.setSummaryText(product.getDescription());
        snapshot.setBasePrice(product.getPrice());
        snapshot.setDisplayPrice(product.getPrice());
        snapshot.setCurrency("CNY");
        snapshot.setCoverImage(extractCoverImage(product.getImageUrls()));

        if (product.getLatitude() != null) {
            snapshot.setLat(BigDecimal.valueOf(product.getLatitude()));
        }
        if (product.getLongitude() != null) {
            snapshot.setLng(BigDecimal.valueOf(product.getLongitude()));
        }

        if (product.getCategoryId() != null) {
            snapshot.setCategoryId(product.getCategoryId().longValue());
            enrichCategory(snapshot, product.getCategoryId().longValue());
        } else {
            snapshot.setCategoryId(0L);
            snapshot.setCategoryName("Uncategorized");
            snapshot.setCategoryPath("0");
        }

        mapProductStatus(snapshot, product.getStatus());

        SearchableStatus searchableStatus = statusCalculator.calculateProduct(
                snapshot.getPublishStatus(),
                snapshot.getAuditStatus(),
                snapshot.getVisibleStatus(),
                snapshot.getSourceStatus()
        );
        snapshot.setSearchableStatus(searchableStatus.getCode());

        snapshot.setSalesCount(0L);
        snapshot.setRating(BigDecimal.ZERO);
        snapshot.setReviewCount(0L);
        snapshot.setHotScore(BigDecimal.ZERO);
        snapshot.setRecommendScore(BigDecimal.ZERO);

        snapshot.setCreatedAt(product.getCreatedAt() != null ? product.getCreatedAt() : LocalDateTime.now());
        snapshot.setUpdatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt() : LocalDateTime.now());
        snapshot.setSearchableUpdatedAt(LocalDateTime.now());
        return snapshot;
    }

    public ProductSearchSnapshot buildAndSave(Product product) {
        ProductSearchSnapshot snapshot = buildFromProduct(product);

        ProductSearchSnapshot existing = productSearchSnapshotMapper.selectByProductId(product.getId());
        if (existing != null) {
            snapshot.setId(existing.getId());
            productSearchSnapshotMapper.updateByProductId(snapshot);
            log.info("updated product snapshot: productId={}", product.getId());
        } else {
            productSearchSnapshotMapper.insert(snapshot);
            log.info("created product snapshot: productId={}", product.getId());
        }

        ProductSearchEsSyncService syncService = productSearchEsSyncServiceProvider.getIfAvailable();
        if (syncService != null) {
            syncService.upsertSnapshot(snapshot);
        }
        return snapshot;
    }

    private void enrichCategory(ProductSearchSnapshot snapshot, Long categoryId) {
        SearchCategory category = searchCategoryMapper.selectById(categoryId);
        if (category != null) {
            snapshot.setCategoryName(category.getCategoryName());
            snapshot.setCategoryPath(category.getCategoryPath());
        } else {
            snapshot.setCategoryName("Uncategorized");
            snapshot.setCategoryPath(String.valueOf(categoryId));
        }
    }

    private void mapProductStatus(ProductSearchSnapshot snapshot, String productStatus) {
        if (!StringUtils.hasText(productStatus)) {
            snapshot.setPublishStatus("off_shelf");
            snapshot.setAuditStatus("pending");
            snapshot.setVisibleStatus("hidden");
            snapshot.setSourceStatus("normal");
            return;
        }

        String normalizedStatus = productStatus.trim().toLowerCase(Locale.ROOT);
        switch (normalizedStatus) {
            case "active":
            case "on_sale":
            case "on_shelf":
            case "onshelf":
            case "1":
            case "\u5728\u552e":
                snapshot.setSourceStatus("normal");
                snapshot.setPublishStatus("on_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("visible");
                break;
            case "inactive":
            case "off_sale":
            case "off_shelf":
            case "offshelf":
            case "0":
            case "\u4e0b\u67b6":
                snapshot.setSourceStatus("normal");
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("hidden");
                break;
            case "pending":
            case "review":
            case "\u5f85\u5ba1\u6838":
            case "\u5ba1\u6838\u4e2d":
                snapshot.setSourceStatus("normal");
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
            case "rejected":
            case "\u9a73\u56de":
            case "\u62d2\u7edd":
                snapshot.setSourceStatus("normal");
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("rejected");
                snapshot.setVisibleStatus("hidden");
                break;
            case "deleted":
            case "disabled":
            case "\u5220\u9664":
            case "\u7981\u7528":
                snapshot.setSourceStatus(normalizedStatus);
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("hidden");
                break;
            default:
                snapshot.setSourceStatus("normal");
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
        }
    }

    private String extractCoverImage(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return null;
        }
        String trimmed = imageUrls.trim();
        if (!trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            List<String> urls = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            if (urls == null) {
                return null;
            }
            for (String url : urls) {
                if (StringUtils.hasText(url)) {
                    return url.trim();
                }
            }
            return null;
        } catch (Exception ex) {
            return trimmed;
        }
    }
}

package com.example.demo.demos.search.snapshot;

import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.common.enums.SearchableStatus;
import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.entity.SearchCategory;
import com.example.demo.demos.search.mapper.ProductSearchSnapshotMapper;
import com.example.demo.demos.search.mapper.SearchCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品快照构建器 — 从 Product + Category + Tag 汇总构建 ProductSearchSnapshot。
 * 与施工单 W02 对齐。
 */
@Component
public class ProductSnapshotBuilder {

    private static final Logger log = LoggerFactory.getLogger(ProductSnapshotBuilder.class);

    @Autowired
    private SearchCategoryMapper searchCategoryMapper;

    @Autowired
    private ProductSearchSnapshotMapper productSearchSnapshotMapper;

    @Autowired
    private SearchableStatusCalculator statusCalculator;

    /**
     * 从现有 Product 构建搜索快照并写入快照表。
     * 字段映射：Product → ProductSearchSnapshot
     *
     * @param product 源商品对象
     * @return 构建好的快照对象
     */
    public ProductSearchSnapshot buildFromProduct(Product product) {
        ProductSearchSnapshot snapshot = new ProductSearchSnapshot();

        // 基础字段映射
        snapshot.setProductId(product.getId());
        snapshot.setSellerId(product.getSellerId() != null ? product.getSellerId().longValue() : 0L);
        snapshot.setTitle(product.getTitle());
        snapshot.setSummaryText(product.getDescription());
        snapshot.setBasePrice(product.getPrice());
        snapshot.setDisplayPrice(product.getPrice());
        snapshot.setCurrency("CNY");
        snapshot.setCoverImage(product.getImageUrls());

        // 地理位置
        if (product.getLatitude() != null) {
            snapshot.setLat(BigDecimal.valueOf(product.getLatitude()));
        }
        if (product.getLongitude() != null) {
            snapshot.setLng(BigDecimal.valueOf(product.getLongitude()));
        }

        // 类目关联
        if (product.getCategoryId() != null) {
            snapshot.setCategoryId(product.getCategoryId().longValue());
            enrichCategory(snapshot, product.getCategoryId().longValue());
        } else {
            snapshot.setCategoryId(0L);
            snapshot.setCategoryName("未分类");
            snapshot.setCategoryPath("0");
        }

        // 状态映射：Product.status → publish/audit/visible
        mapProductStatus(snapshot, product.getStatus());

        // 计算 searchable_status
        SearchableStatus searchableStatus = statusCalculator.calculateProduct(
                snapshot.getPublishStatus(),
                snapshot.getAuditStatus(),
                snapshot.getVisibleStatus(),
                snapshot.getSourceStatus()
        );
        snapshot.setSearchableStatus(searchableStatus.getCode());

        // 默认评分字段
        snapshot.setSalesCount(0L);
        snapshot.setRating(BigDecimal.ZERO);
        snapshot.setReviewCount(0L);
        snapshot.setHotScore(BigDecimal.ZERO);
        snapshot.setRecommendScore(BigDecimal.ZERO);

        // 时间
        snapshot.setCreatedAt(product.getCreatedAt() != null ? product.getCreatedAt() : LocalDateTime.now());
        snapshot.setUpdatedAt(product.getUpdatedAt() != null ? product.getUpdatedAt() : LocalDateTime.now());
        snapshot.setSearchableUpdatedAt(LocalDateTime.now());

        return snapshot;
    }

    /**
     * 构建快照并写入数据库（upsert 语义）。
     */
    public ProductSearchSnapshot buildAndSave(Product product) {
        ProductSearchSnapshot snapshot = buildFromProduct(product);

        ProductSearchSnapshot existing = productSearchSnapshotMapper.selectByProductId(product.getId());
        if (existing != null) {
            snapshot.setId(existing.getId());
            productSearchSnapshotMapper.updateByProductId(snapshot);
            log.info("更新商品快照: productId={}", product.getId());
        } else {
            productSearchSnapshotMapper.insert(snapshot);
            log.info("新建商品快照: productId={}", product.getId());
        }

        return snapshot;
    }

    /**
     * 从 category 表补充类目名称和路径。
     */
    private void enrichCategory(ProductSearchSnapshot snapshot, Long categoryId) {
        SearchCategory category = searchCategoryMapper.selectById(categoryId);
        if (category != null) {
            snapshot.setCategoryName(category.getCategoryName());
            snapshot.setCategoryPath(category.getCategoryPath());
        } else {
            snapshot.setCategoryName("未分类");
            snapshot.setCategoryPath(String.valueOf(categoryId));
        }
    }

    /**
     * 将现有 Product.status 映射到快照的多维度状态。
     * 现有 Product.status 可能的值：active/inactive/deleted/pending 等。
     */
    private void mapProductStatus(ProductSearchSnapshot snapshot, String productStatus) {
        if (productStatus == null) {
            snapshot.setPublishStatus("off_shelf");
            snapshot.setAuditStatus("pending");
            snapshot.setVisibleStatus("hidden");
            snapshot.setSourceStatus(null);
            return;
        }

        snapshot.setSourceStatus(productStatus);

        switch (productStatus.toLowerCase()) {
            case "active":
            case "on_sale":
                snapshot.setPublishStatus("on_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("visible");
                break;
            case "inactive":
            case "off_sale":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("hidden");
                break;
            case "pending":
            case "review":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
            case "rejected":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("rejected");
                snapshot.setVisibleStatus("hidden");
                break;
            case "deleted":
            case "disabled":
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("approved");
                snapshot.setVisibleStatus("hidden");
                break;
            default:
                snapshot.setPublishStatus("off_shelf");
                snapshot.setAuditStatus("pending");
                snapshot.setVisibleStatus("hidden");
                break;
        }
    }
}

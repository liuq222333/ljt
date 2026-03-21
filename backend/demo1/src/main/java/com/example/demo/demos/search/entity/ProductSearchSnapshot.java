package com.example.demo.demos.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品搜索快照实体 — 对应 product_search_snapshot 表。
 * 结构化查询投影层，字段扁平冗余，面向搜索场景。
 */
@Data
public class ProductSearchSnapshot {

    private Long id;
    private Long productId;
    private Long sellerId;
    private Long storeId;
    private String storeName;
    private String productType;
    private String title;
    private String subtitle;
    private String summaryText;
    private Long categoryId;
    private String categoryName;
    private String categoryPath;
    /** 标签ID数组JSON，如 [1001,1002] */
    private String tagIds;
    /** 标签名称数组JSON，如 ["美食","亲子"] */
    private String tagNames;
    private Long cityId;
    private String cityName;
    private Long districtId;
    private String districtName;
    private Long businessAreaId;
    private String businessAreaName;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal basePrice;
    private BigDecimal displayPrice;
    private String currency;
    private String coverImage;
    private Long salesCount;
    private BigDecimal rating;
    private Long reviewCount;
    private BigDecimal hotScore;
    private BigDecimal recommendScore;
    /** 搜索状态：searchable/not_searchable/expired/draft/disabled */
    private String searchableStatus;
    /** 上架状态：on_shelf/off_shelf */
    private String publishStatus;
    /** 审核状态：pending/approved/rejected */
    private String auditStatus;
    /** 可见状态：visible/hidden */
    private String visibleStatus;
    private String sourceStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime searchableUpdatedAt;
}

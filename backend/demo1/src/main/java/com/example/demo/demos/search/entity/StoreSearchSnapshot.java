package com.example.demo.demos.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店搜索快照实体 — 对应 store_search_snapshot 表。
 */
@Data
public class StoreSearchSnapshot {

    private Long id;
    private Long storeId;
    private Long merchantId;
    private String storeName;
    private String title;
    private String summaryText;
    private Long categoryId;
    private String categoryName;
    private String categoryPath;
    private String tagNames;
    private Long cityId;
    private Long districtId;
    private Long businessAreaId;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal avgPrice;
    private BigDecimal rating;
    private Long reviewCount;
    private BigDecimal hotScore;
    private String searchableStatus;
    private String publishStatus;
    private String visibleStatus;
    private String baseOpenTimeDesc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

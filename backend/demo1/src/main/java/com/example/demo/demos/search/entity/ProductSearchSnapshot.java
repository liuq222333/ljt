package com.example.demo.demos.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String tagIds;
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
    private Double distanceKm;
    private Integer keywordScore;
    private String searchableStatus;
    private String publishStatus;
    private String auditStatus;
    private String visibleStatus;
    private String sourceStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime searchableUpdatedAt;
}

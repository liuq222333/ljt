package com.example.demo.demos.search.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动搜索快照实体 — 对应 event_search_snapshot 表。
 */
@Data
public class EventSearchSnapshot {

    private Long id;
    private Long eventId;
    private Long organizerId;
    private Long venueId;
    private String venueName;
    private String title;
    private String subtitle;
    private String summaryText;
    private Long categoryId;
    private String categoryName;
    private String categoryPath;
    private String tagIds;
    private String tagNames;
    private String audienceTags;
    private Long cityId;
    private String cityName;
    private Long districtId;
    private String districtName;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime eventStartTime;
    private LocalDateTime eventEndTime;
    private String weekdayMask;
    private String searchableStatus;
    private String publishStatus;
    private String auditStatus;
    private String visibleStatus;
    private Long salesCount;
    private BigDecimal rating;
    private BigDecimal hotScore;
    private BigDecimal recommendScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime searchableUpdatedAt;
}

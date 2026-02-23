package com.example.demo.demos.CommunityMarket.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductNearbyDTO {
    private Long id;
    private Integer sellerId;
    private Integer categoryId;
    private String title;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String condition;
    private String location;
    private String imageUrls;
    private String status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String distanceSource;
}

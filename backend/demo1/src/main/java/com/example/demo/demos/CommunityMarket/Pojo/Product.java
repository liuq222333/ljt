package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

@Data
public class Product {
    private Long id;
    private Integer sellerId;
    private Integer categoryId;
    private String title;
    private String description;
    private java.math.BigDecimal price;
    private Integer stockQuantity;
    private String condition;
    private String location;
    private String imageUrls;
    private String status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;

}

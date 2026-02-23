package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

@Data
public class ProductImages {
    private Long id;
    private Long productId;
    private String imageUrl;
    private java.time.LocalDateTime createdAt;
    private Integer sortOrder;
}

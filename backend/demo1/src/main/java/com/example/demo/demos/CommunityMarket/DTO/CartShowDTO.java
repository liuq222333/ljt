package com.example.demo.demos.CommunityMarket.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CartShowDTO {
    private int quantity;
    private double price;
    private String title;
    private String imageUrls;
    private LocalDateTime createdAt;
    private Long productId;
    private String status;
    private Integer stockQuantity;
}

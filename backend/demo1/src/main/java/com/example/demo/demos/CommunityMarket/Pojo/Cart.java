package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

@Data
public class Cart {
    private Long id;
    private String userName;
    private Long productId;
    private int quantity;
    private java.time.LocalDateTime createdAt;
    private java.math.BigDecimal price;
}

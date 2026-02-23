package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

@Data
public class UserProducts {
    private Long id;
    private Long userId;
    private Long productId;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}

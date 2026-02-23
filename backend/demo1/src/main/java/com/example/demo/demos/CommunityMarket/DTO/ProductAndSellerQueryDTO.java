package com.example.demo.demos.CommunityMarket.DTO;

import lombok.Data;

@Data
public class ProductAndSellerQueryDTO {
    private String id;
    private  String sellerId;
    private String categoryId;
    private String title;
    private String description;
    private String price;
    private String stockQuantity;
    private String condition;
    private String location;
    private String imageUrls;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String userId;
    private String username;
    private String avatarUrl;
    private String email;
    private String phone;
    private String address;
    private Double latitude;
    private Double longitude;

}

package com.example.demo.demos.CommunityMarket.DTO;

import lombok.Data;

@Data
public class ProductQueryDTO {
    private String keyword;
    private String categoryId;
    private String subCategoryId;
    private String brand;
    private Integer priceMin;
    private Integer priceMax;
    private Boolean withImage;
    private Boolean freeShipping;
    private Boolean invoice;
    private Integer minRating;
    private String sort;   // comprehensive | latest | price
    private String order;  // asc | desc
    private Integer page;
    private Integer size;
}
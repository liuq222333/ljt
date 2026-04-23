package com.example.demo.demos.search.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductSearchQuery {

    private String keyword;
    private Long categoryId;
    private Long cityId;
    private Long districtId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> tagNames = new ArrayList<String>();
    private Boolean searchableOnly = Boolean.TRUE;
    private String sortBy = "recommend_score";
    private Integer page = 1;
    private Integer size = 20;

    public int offset() {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safeSize = size == null || size <= 0 ? 20 : size;
        return (safePage - 1) * safeSize;
    }

    public int limit() {
        return size == null || size <= 0 ? 20 : size;
    }
}

package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

@Data
public class Category {
    private String id;
    private String name;
    private String parentId;
    private String iconUrl;
    private String sortOrder;
}

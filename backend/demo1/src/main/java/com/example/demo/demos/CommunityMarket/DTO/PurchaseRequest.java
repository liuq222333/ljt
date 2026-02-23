package com.example.demo.demos.CommunityMarket.DTO;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseRequest {
    private String userName;
    private List<PurchaseItem> items;
}


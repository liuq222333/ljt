package com.example.demo.demos.CommunityFeed.DTO;

import lombok.Data;

import java.util.List;

@Data
public class CommunityFeedPostRequest {
    private Long userId;
    private String content;
    private List<String> images; // MinIO 对象 key 或可访问 URL
    private String visibility;   // PUBLIC/PRIVATE 等
    private String locationText;
    private List<String> topics; // 话题列表（简单存储为 JSON）
}

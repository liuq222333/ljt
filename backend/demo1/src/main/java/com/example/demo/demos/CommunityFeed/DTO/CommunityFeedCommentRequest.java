package com.example.demo.demos.CommunityFeed.DTO;

import lombok.Data;

@Data
public class CommunityFeedCommentRequest {
    private Long userId;
    private String content;
}

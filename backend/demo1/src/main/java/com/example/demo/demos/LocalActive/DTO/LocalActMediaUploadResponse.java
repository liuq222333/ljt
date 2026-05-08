package com.example.demo.demos.LocalActive.DTO;

import lombok.Data;

@Data
public class LocalActMediaUploadResponse {
    private String objectKey;
    private String url;
    private String publicUrl;
    private String contentType;
    private long size;
}

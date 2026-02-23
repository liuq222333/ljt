package com.example.demo.demosAdmin.ApiManagement.Pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiRoute {
    private Long id;
    private String resource;
    private String action;
    private String httpMethod;
    private String pathTemplate;
    private String pathParams; // Storing JSON as String for simplicity
    private String operationType;
    private String description;
    private Integer enabled; // 1: enabled, 0: disabled
    private String querySchema; // Storing JSON as String
    private String bodySchema; // Storing JSON as String
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

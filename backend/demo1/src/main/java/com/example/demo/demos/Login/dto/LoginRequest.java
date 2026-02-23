package com.example.demo.demos.Login.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String phone;
}
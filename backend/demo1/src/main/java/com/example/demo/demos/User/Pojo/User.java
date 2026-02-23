package com.example.demo.demos.User.Pojo;

import lombok.Data;

@Data
public class User {
    private String userId;
    private String userName;
    private String password;
    private String email;
    private String phone;
    private String address;
    private String avatarKey;
    // private String avatarUrl;
    private Double latitude;
    private Double longitude;
}

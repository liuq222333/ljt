package com.example.demo.demos.User.DTO;

import lombok.Data;

@Data
public class UserInfoDTO {
    private String userName;
    private String email;
    private String phone;
    private String address;
    private String userId;
    private Double latitude;
    private Double longitude;
}

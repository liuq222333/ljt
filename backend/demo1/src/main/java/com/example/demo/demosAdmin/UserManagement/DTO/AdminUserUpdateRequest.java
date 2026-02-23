package com.example.demo.demosAdmin.UserManagement.DTO;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String userName;
    private String email;
    private String phone;
    private String address;
}

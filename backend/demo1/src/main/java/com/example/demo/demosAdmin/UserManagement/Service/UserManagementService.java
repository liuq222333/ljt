package com.example.demo.demosAdmin.UserManagement.Service;

import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserDTO;
import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserUpdateRequest;

import java.util.List;

public interface UserManagementService {

    List<AdminUserDTO> listUsers(String keyword, int page, int size);

    void updateUser(String userId, AdminUserUpdateRequest request);

    void resetPassword(String userId, String newPassword);

    void deleteUser(String userId);
}

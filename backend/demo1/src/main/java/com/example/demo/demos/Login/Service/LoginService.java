package com.example.demo.demos.Login.Service;

import com.example.demo.demos.Login.Entity.User;

public interface LoginService {
    User getUserByName(String userName);

    boolean register(String userName, String rawPassword, String phone);

    boolean validateLogin(String userName, String rawPassword);

    User getUserById(String userId);

    boolean changePassword(String userId, String oldPassword, String newPassword);
}

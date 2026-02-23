package com.example.demo.demos.Login.Service.Impl;

import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Mapper.LoginMapper;
import com.example.demo.demos.Login.Service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private LoginMapper loginMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User getUserByName(String userName) {
        return loginMapper.getUserByName(userName);
    }

    @Override
    public boolean register(String userName, String rawPassword, String phone) {
        String encoded = passwordEncoder.encode(rawPassword);
        return loginMapper.insertUser(userName, encoded, phone) > 0;
    }

    @Override
    public boolean validateLogin(String userName, String rawPassword) {
        User user = loginMapper.getUserByName(userName);
        if (user == null || user.getPassword() == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    public User getUserById(String userId) {
        return loginMapper.getUserById(userId);
    }

    @Override
    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = loginMapper.getUserById(userId);
        if (user == null || user.getPassword() == null) {
            return false;
        }
        //比较新旧密码是否一致
        boolean matches = passwordEncoder.matches(oldPassword, user.getPassword());
        if (!matches) {
            return false;
        }
        //将新密码进行加密并存入数据库
        String encodedNew = passwordEncoder.encode(newPassword);
        return loginMapper.updatePasswordByUserId(userId, encodedNew) > 0;
    }
}

package com.example.demo.demos.User.Service.Impl;

import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Mapper.LoginMapper;
import com.example.demo.demos.User.DTO.UserInfoDTO;
import com.example.demo.demos.User.Dao.UserMapper;
import com.example.demo.demos.User.Service.UserService;
import com.example.demo.demos.exception.ResourceConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private LoginMapper loginMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void updateUser(String userId, String userName) {
        userMapper.updateUser(userId, userName);
    }

    @Override
    public void updateUserInfo(UserInfoDTO userInfoDTO) {
        // Check if the email is being changed to an email that already exists for another user.
        User userWithEmail = userMapper.getUserEmailIsExist(userInfoDTO.getEmail());
        if(userWithEmail != null && !userWithEmail.getUserId().equals(userInfoDTO.getUserId())){
            throw new ResourceConflictException("邮箱已存在");
        }

        log.info(userInfoDTO.getLatitude().toString());
        log.info(userInfoDTO.getLongitude().toString());
        userMapper.updateUserInfo(userInfoDTO);
    }

    @Override
    public void deleteUser(String userId, String password) {
        User user = loginMapper.getUserById(userId);
        if(user == null || user.getPassword() == null){
            throw new ResourceConflictException("用户不存在");
        }
        boolean ok = passwordEncoder.matches(password, user.getPassword());
        if(ok){
            userMapper.deleteUser(userId);
        }
    }

    @Override
    public Integer getUserIdByName(String userName) {
        return userMapper.getUserIdByName(userName);
    }

    @Override
    public com.example.demo.demos.User.Pojo.User findById(String userId) {
        return userMapper.findById(userId);
    }
}

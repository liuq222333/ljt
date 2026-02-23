package com.example.demo.demos.User.Service;

import com.example.demo.demos.User.DTO.UserInfoDTO;
import com.example.demo.demos.User.Pojo.User;
import org.springframework.http.ResponseEntity;

public interface UserService {

    void updateUser(String userId, String userName);

    void updateUserInfo(UserInfoDTO userInfoDTO);

    void deleteUser(String userId,String password);

    Integer getUserIdByName(String userName);

    User findById(String userId);
}

package com.example.demo.demos.User.Dao;

import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.User.DTO.UserInfoDTO;
import org.apache.ibatis.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Mapper
public interface UserMapper {

    @Update("update users set username = #{userName} where user_id = #{userId}")
    void updateUser(@Param("userId") String userId, @Param("userName") String userName);

    @Update("update users set username = #{userName}," +
            " email = #{email}, phone = #{phone}," +
            " address = #{address}, " +
            " latitude = #{latitude}, " +
            " longitude = #{longitude} " +
            "where user_id = #{userId}")
    void updateUserInfo(UserInfoDTO userInfoDTO);

    @Select("SELECT user_id FROM users WHERE email = #{email}")
    User getUserEmailIsExist(String email);

    @Delete("DELETE FROM users WHERE user_id = #{userId}")
    void deleteUser(String userId);

    @Select("SELECT user_id FROM users WHERE username = #{userName}")
    Integer getUserIdByName(String userName);

    @Select("SELECT * FROM users WHERE users.user_id = #{userId}")
    com.example.demo.demos.User.Pojo.User findById(String userId);
}

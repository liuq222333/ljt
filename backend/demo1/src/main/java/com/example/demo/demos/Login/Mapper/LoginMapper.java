package com.example.demo.demos.Login.Mapper;

import com.example.demo.demos.Login.Entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LoginMapper {

    @Select("SELECT * FROM users WHERE userName = #{userName}")
    User getUserByName(@Param("userName") String userName);

    @Insert("INSERT INTO users (userName, password, phone) VALUES (#{userName}, #{password}, #{phone})")
    int insertUser(@Param("userName") String userName, @Param("password") String password, @Param("phone") String phone);

    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    User getUserById(String userId);

    @Update("UPDATE users SET password = #{password} WHERE user_id = #{userId}")
    int updatePasswordByUserId(@Param("userId") String userId, @Param("password") String password);
}

package com.example.demo.demosAdmin.UserManagement.Dao;

import com.example.demo.demosAdmin.UserManagement.DTO.AdminUserDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminUserMapper {

    @Select({
            "<script>",
            "SELECT user_id AS userId, userName AS userName, email, phone, address",
            "FROM users",
            "WHERE 1=1",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (userName LIKE CONCAT('%', #{keyword}, '%')",
            "         OR email LIKE CONCAT('%', #{keyword}, '%')",
            "         OR phone LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "ORDER BY user_id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AdminUserDTO> listUsers(@Param("keyword") String keyword,
                                 @Param("limit") int limit,
                                 @Param("offset") int offset);

    @Update("UPDATE users SET userName = #{userName}, email = #{email}, phone = #{phone}, address = #{address} WHERE user_id = #{userId}")
    int updateUser(AdminUserDTO user);

    @Update("UPDATE users SET password = #{password} WHERE user_id = #{userId}")
    int resetPassword(@Param("userId") String userId, @Param("password") String password);

    @Delete("DELETE FROM users WHERE user_id = #{userId}")
    int deleteUser(@Param("userId") String userId);
}

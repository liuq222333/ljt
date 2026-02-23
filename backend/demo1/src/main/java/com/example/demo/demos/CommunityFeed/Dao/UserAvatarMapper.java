package com.example.demo.demos.CommunityFeed.Dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAvatarMapper {

    @Select("SELECT avatar_key FROM users WHERE user_id = #{id}")
    String findAvatarKey(@Param("id") Long id);

    @Update("UPDATE users SET avatar_key = #{avatarKey} WHERE user_id = #{id}")
    int updateAvatarKey(@Param("id") String id, @Param("avatarKey") String avatarKey);
}

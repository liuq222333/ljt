package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocalActivityMapper {
    int insertActivity(LocalActivity activity);

    int insertTags(@Param("activityId") Long activityId, @Param("tags") List<String> tags);

    List<LocalActivity> listActivities(@Param("status") String status,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    List<LocalActivity> listAll();
}

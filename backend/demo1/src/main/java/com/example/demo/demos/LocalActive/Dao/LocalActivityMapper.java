package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.DTO.LocalActivityDetail;
import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocalActivityMapper {
    int insertActivity(LocalActivity activity);

    int insertTags(@Param("activityId") Long activityId, @Param("tags") List<String> tags);

    List<LocalActivity> listActivities(@Param("status") String status,
                                       @Param("timeState") String timeState,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    List<LocalActivity> listActivitiesByOrganizer(@Param("organizerUserId") Integer organizerUserId,
                                                  @Param("status") String status,
                                                  @Param("timeState") String timeState,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    List<LocalActivity> listFavoriteActivities(@Param("userId") Integer userId,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    List<LocalActivity> listAll();

    LocalActivityDetail findDetailById(@Param("id") Long id);

    List<String> listTags(@Param("activityId") Long activityId);

    String findUserEnrollmentStatus(@Param("activityId") Long activityId,
                                    @Param("username") String username);

    int countFavorite(@Param("activityId") Long activityId, @Param("userId") Integer userId);

    int insertFavorite(@Param("activityId") Long activityId, @Param("userId") Integer userId);

    int deleteFavorite(@Param("activityId") Long activityId, @Param("userId") Integer userId);
}

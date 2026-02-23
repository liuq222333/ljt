package com.example.demo.demosAdmin.AdminNeighborSupportTask.Dao;

import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminNeighborSupportTaskMapper {

    @Select("<script>" +
            "SELECT * FROM neighbor_support_task_admin " +
            "WHERE 1=1 " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "<if test='keyword != null'> AND (title LIKE CONCAT('%',#{keyword},'%') OR description LIKE CONCAT('%',#{keyword},'%') OR location_text LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<NeighborSupportTask> listAdminTasks(@Param("status") String status,
                                             @Param("keyword") String keyword,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    @Select("SELECT * FROM neighbor_support_task_admin WHERE id = #{id}")
    NeighborSupportTask findAdminTaskById(@Param("id") Long id);

    @Insert("INSERT INTO neighbor_support_task (" +
            "requester_user_id, assignee_user_id, title, category_code, description, " +
            "location_text, latitude, longitude, start_time, end_time, " +
            "volunteer_slots, filled_slots, priority, reward_points, status, created_at, updated_at) " +
            "VALUES (" +
            "#{requesterUserId}, #{assigneeUserId}, #{title}, #{categoryCode}, #{description}, " +
            "#{locationText}, #{latitude}, #{longitude}, #{startTime}, #{endTime}, " +
            "#{volunteerSlots}, #{filledSlots}, #{priority}, #{rewardPoints}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertToMain(NeighborSupportTask task);

    @Delete("DELETE FROM neighbor_support_task_admin WHERE id = #{id}")
    int deleteAdminTask(@Param("id") Long id);

    @Update("UPDATE neighbor_support_task_admin SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateAdminStatus(@Param("id") Long id, @Param("status") String status);
}

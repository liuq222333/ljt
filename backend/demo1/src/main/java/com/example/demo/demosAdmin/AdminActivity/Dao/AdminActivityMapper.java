package com.example.demo.demosAdmin.AdminActivity.Dao;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminActivityMapper {

    @Select({
            "<script>",
            "SELECT id, organizer_user_id, title, subtitle, category_code, description,",
            "       location_text, address, latitude, longitude, cover_url, capacity, fee_type, fee_amount, allow_waitlist, require_checkin,",
            "       status, start_at, end_at, reminder_minutes, review_note",
            "  FROM local_activity",
            " WHERE 1=1",
            " <if test='status != null and status != \"\"'> AND status = #{status} </if>",
            " <if test='keyword != null and keyword != \"\"'>",
            "   AND (title LIKE CONCAT('%',#{keyword},'%')",
            "        OR description LIKE CONCAT('%',#{keyword},'%')",
            "        OR location_text LIKE CONCAT('%',#{keyword},'%'))",
            " </if>",
            " ORDER BY created_at DESC",
            " LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<LocalActivity> listAdminActivities(@Param("status") String status,
                                            @Param("keyword") String keyword,
                                            @Param("limit") int limit,
                                            @Param("offset") int offset);

    @Select("SELECT id, organizer_user_id, title, subtitle, category_code, description, location_text, address, latitude, longitude, cover_url, capacity, fee_type, fee_amount, allow_waitlist, require_checkin, status, start_at, end_at, reminder_minutes, review_note FROM local_activity WHERE id = #{id}")
    LocalActivity findActivityById(@Param("id") Long id);

    @Update("UPDATE local_activity SET status = #{status}, review_note = #{note}, updated_at = NOW() WHERE id = #{id}")
    int updateActivityStatus(@Param("id") Long id, @Param("status") String status, @Param("note") String note);

    @Select("SELECT tag FROM local_activity_tag WHERE activity_id = #{activityId}")
    List<String> listTags(@Param("activityId") Long activityId);

    @Insert("INSERT INTO local_activity_tag(activity_id, tag) VALUES(#{activityId}, #{tag})")
    int insertTag(@Param("activityId") Long activityId, @Param("tag") String tag);

    @Delete("DELETE FROM local_activity_tag WHERE activity_id = #{activityId}")
    int deleteTags(@Param("activityId") Long activityId);
}

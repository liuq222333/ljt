package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.EventSearchSnapshot;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动搜索快照 Mapper — 对应 event_search_snapshot 表。
 */
@Mapper
public interface EventSearchSnapshotMapper {

    @Insert("INSERT INTO event_search_snapshot (" +
            "event_id, organizer_id, venue_id, venue_name, title, subtitle, summary_text, " +
            "category_id, category_name, category_path, tag_ids, tag_names, audience_tags, " +
            "city_id, city_name, district_id, district_name, lat, lng, " +
            "min_price, max_price, event_start_time, event_end_time, weekday_mask, " +
            "searchable_status, publish_status, audit_status, visible_status, " +
            "sales_count, rating, hot_score, recommend_score, " +
            "created_at, updated_at, searchable_updated_at" +
            ") VALUES (" +
            "#{eventId}, #{organizerId}, #{venueId}, #{venueName}, #{title}, #{subtitle}, #{summaryText}, " +
            "#{categoryId}, #{categoryName}, #{categoryPath}, #{tagIds}, #{tagNames}, #{audienceTags}, " +
            "#{cityId}, #{cityName}, #{districtId}, #{districtName}, #{lat}, #{lng}, " +
            "#{minPrice}, #{maxPrice}, #{eventStartTime}, #{eventEndTime}, #{weekdayMask}, " +
            "#{searchableStatus}, #{publishStatus}, #{auditStatus}, #{visibleStatus}, " +
            "#{salesCount}, #{rating}, #{hotScore}, #{recommendScore}, " +
            "#{createdAt}, #{updatedAt}, #{searchableUpdatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EventSearchSnapshot snapshot);

    @Select("SELECT * FROM event_search_snapshot WHERE event_id = #{eventId}")
    EventSearchSnapshot selectByEventId(@Param("eventId") Long eventId);

    @Select("SELECT * FROM event_search_snapshot WHERE searchable_status = 'searchable' " +
            "AND event_end_time > NOW() ORDER BY event_start_time ASC LIMIT #{limit}")
    List<EventSearchSnapshot> selectUpcomingSearchable(@Param("limit") int limit);

    @Select("SELECT * FROM event_search_snapshot WHERE updated_at > #{since} " +
            "ORDER BY updated_at ASC")
    List<EventSearchSnapshot> selectUpdatedSince(@Param("since") LocalDateTime since);

    @Delete("DELETE FROM event_search_snapshot WHERE event_id = #{eventId}")
    int deleteByEventId(@Param("eventId") Long eventId);

    @Update("UPDATE event_search_snapshot SET " +
            "organizer_id = #{organizerId}, venue_id = #{venueId}, venue_name = #{venueName}, " +
            "title = #{title}, subtitle = #{subtitle}, summary_text = #{summaryText}, " +
            "category_id = #{categoryId}, category_name = #{categoryName}, category_path = #{categoryPath}, " +
            "tag_ids = #{tagIds}, tag_names = #{tagNames}, audience_tags = #{audienceTags}, " +
            "city_id = #{cityId}, city_name = #{cityName}, district_id = #{districtId}, " +
            "district_name = #{districtName}, lat = #{lat}, lng = #{lng}, " +
            "min_price = #{minPrice}, max_price = #{maxPrice}, " +
            "event_start_time = #{eventStartTime}, event_end_time = #{eventEndTime}, " +
            "weekday_mask = #{weekdayMask}, " +
            "searchable_status = #{searchableStatus}, publish_status = #{publishStatus}, " +
            "audit_status = #{auditStatus}, visible_status = #{visibleStatus}, " +
            "sales_count = #{salesCount}, rating = #{rating}, hot_score = #{hotScore}, " +
            "recommend_score = #{recommendScore}, " +
            "updated_at = #{updatedAt}, searchable_updated_at = #{searchableUpdatedAt} " +
            "WHERE event_id = #{eventId}")
    int updateByEventId(EventSearchSnapshot snapshot);

    @Select("SELECT COUNT(*) FROM event_search_snapshot")
    long countAll();

    @Select("SELECT COUNT(*) FROM event_search_snapshot WHERE searchable_status = 'searchable'")
    long countSearchable();
}

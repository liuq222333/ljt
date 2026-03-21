package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.StoreSearchSnapshot;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店搜索快照 Mapper — 对应 store_search_snapshot 表。
 */
@Mapper
public interface StoreSearchSnapshotMapper {

    @Insert("INSERT INTO store_search_snapshot (" +
            "store_id, merchant_id, store_name, title, summary_text, " +
            "category_id, category_name, category_path, tag_names, " +
            "city_id, district_id, business_area_id, lat, lng, " +
            "avg_price, rating, review_count, hot_score, " +
            "searchable_status, publish_status, visible_status, base_open_time_desc, " +
            "created_at, updated_at" +
            ") VALUES (" +
            "#{storeId}, #{merchantId}, #{storeName}, #{title}, #{summaryText}, " +
            "#{categoryId}, #{categoryName}, #{categoryPath}, #{tagNames}, " +
            "#{cityId}, #{districtId}, #{businessAreaId}, #{lat}, #{lng}, " +
            "#{avgPrice}, #{rating}, #{reviewCount}, #{hotScore}, " +
            "#{searchableStatus}, #{publishStatus}, #{visibleStatus}, #{baseOpenTimeDesc}, " +
            "#{createdAt}, #{updatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StoreSearchSnapshot snapshot);

    @Select("SELECT * FROM store_search_snapshot WHERE store_id = #{storeId}")
    StoreSearchSnapshot selectByStoreId(@Param("storeId") Long storeId);

    @Select("SELECT * FROM store_search_snapshot WHERE searchable_status = 'searchable' " +
            "ORDER BY hot_score DESC LIMIT #{limit}")
    List<StoreSearchSnapshot> selectTopSearchable(@Param("limit") int limit);

    @Select("SELECT * FROM store_search_snapshot WHERE updated_at > #{since} " +
            "ORDER BY updated_at ASC")
    List<StoreSearchSnapshot> selectUpdatedSince(@Param("since") LocalDateTime since);

    @Delete("DELETE FROM store_search_snapshot WHERE store_id = #{storeId}")
    int deleteByStoreId(@Param("storeId") Long storeId);

    @Update("UPDATE store_search_snapshot SET " +
            "merchant_id = #{merchantId}, store_name = #{storeName}, title = #{title}, " +
            "summary_text = #{summaryText}, category_id = #{categoryId}, category_name = #{categoryName}, " +
            "category_path = #{categoryPath}, tag_names = #{tagNames}, " +
            "city_id = #{cityId}, district_id = #{districtId}, business_area_id = #{businessAreaId}, " +
            "lat = #{lat}, lng = #{lng}, avg_price = #{avgPrice}, " +
            "rating = #{rating}, review_count = #{reviewCount}, hot_score = #{hotScore}, " +
            "searchable_status = #{searchableStatus}, publish_status = #{publishStatus}, " +
            "visible_status = #{visibleStatus}, base_open_time_desc = #{baseOpenTimeDesc}, " +
            "updated_at = #{updatedAt} " +
            "WHERE store_id = #{storeId}")
    int updateByStoreId(StoreSearchSnapshot snapshot);

    @Select("SELECT COUNT(*) FROM store_search_snapshot")
    long countAll();

    @Select("SELECT COUNT(*) FROM store_search_snapshot WHERE searchable_status = 'searchable'")
    long countSearchable();
}

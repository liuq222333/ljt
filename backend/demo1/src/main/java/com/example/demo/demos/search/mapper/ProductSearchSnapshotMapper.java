package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.ProductSearchSnapshot;
import com.example.demo.demos.search.model.ProductSearchQuery;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductSearchSnapshotMapper {

    @Insert("INSERT INTO product_search_snapshot (" +
            "product_id, seller_id, store_id, store_name, product_type, title, subtitle, summary_text, " +
            "category_id, category_name, category_path, tag_ids, tag_names, " +
            "city_id, city_name, district_id, district_name, business_area_id, business_area_name, " +
            "lat, lng, base_price, display_price, currency, cover_image, " +
            "sales_count, rating, review_count, hot_score, recommend_score, " +
            "searchable_status, publish_status, audit_status, visible_status, source_status, " +
            "created_at, updated_at, searchable_updated_at" +
            ") VALUES (" +
            "#{productId}, #{sellerId}, #{storeId}, #{storeName}, #{productType}, #{title}, #{subtitle}, #{summaryText}, " +
            "#{categoryId}, #{categoryName}, #{categoryPath}, #{tagIds}, #{tagNames}, " +
            "#{cityId}, #{cityName}, #{districtId}, #{districtName}, #{businessAreaId}, #{businessAreaName}, " +
            "#{lat}, #{lng}, #{basePrice}, #{displayPrice}, #{currency}, #{coverImage}, " +
            "#{salesCount}, #{rating}, #{reviewCount}, #{hotScore}, #{recommendScore}, " +
            "#{searchableStatus}, #{publishStatus}, #{auditStatus}, #{visibleStatus}, #{sourceStatus}, " +
            "#{createdAt}, #{updatedAt}, #{searchableUpdatedAt}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProductSearchSnapshot snapshot);

    @Select("SELECT * FROM product_search_snapshot WHERE product_id = #{productId}")
    ProductSearchSnapshot selectByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM product_search_snapshot WHERE searchable_status = 'searchable' ORDER BY recommend_score DESC LIMIT #{limit}")
    List<ProductSearchSnapshot> selectTopSearchable(@Param("limit") int limit);

    @Select("SELECT * FROM product_search_snapshot WHERE searchable_status = 'searchable' ORDER BY product_id ASC LIMIT #{limit} OFFSET #{offset}")
    List<ProductSearchSnapshot> selectSearchablePage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM product_search_snapshot WHERE updated_at > #{since} ORDER BY updated_at ASC")
    List<ProductSearchSnapshot> selectUpdatedSince(@Param("since") LocalDateTime since);

    @Delete("DELETE FROM product_search_snapshot WHERE product_id = #{productId}")
    int deleteByProductId(@Param("productId") Long productId);

    @Update("UPDATE product_search_snapshot SET " +
            "seller_id = #{sellerId}, store_id = #{storeId}, store_name = #{storeName}, " +
            "product_type = #{productType}, title = #{title}, subtitle = #{subtitle}, " +
            "summary_text = #{summaryText}, category_id = #{categoryId}, category_name = #{categoryName}, " +
            "category_path = #{categoryPath}, tag_ids = #{tagIds}, tag_names = #{tagNames}, " +
            "city_id = #{cityId}, city_name = #{cityName}, district_id = #{districtId}, " +
            "district_name = #{districtName}, business_area_id = #{businessAreaId}, " +
            "business_area_name = #{businessAreaName}, lat = #{lat}, lng = #{lng}, " +
            "base_price = #{basePrice}, display_price = #{displayPrice}, currency = #{currency}, " +
            "cover_image = #{coverImage}, sales_count = #{salesCount}, rating = #{rating}, " +
            "review_count = #{reviewCount}, hot_score = #{hotScore}, recommend_score = #{recommendScore}, " +
            "searchable_status = #{searchableStatus}, publish_status = #{publishStatus}, " +
            "audit_status = #{auditStatus}, visible_status = #{visibleStatus}, source_status = #{sourceStatus}, " +
            "updated_at = #{updatedAt}, searchable_updated_at = #{searchableUpdatedAt} " +
            "WHERE product_id = #{productId}")
    int updateByProductId(ProductSearchSnapshot snapshot);

    @Select("SELECT COUNT(*) FROM product_search_snapshot")
    long countAll();

    @Select("SELECT COUNT(*) FROM product_search_snapshot WHERE searchable_status = 'searchable'")
    long countSearchable();

    @Select("SELECT product_id FROM product_search_snapshot ORDER BY product_id ASC")
    List<Long> selectAllProductIds();

    @Select("SELECT product_id FROM product_search_snapshot WHERE searchable_status = 'searchable' ORDER BY product_id ASC")
    List<Long> selectSearchableProductIds();

    @SelectProvider(type = ProductSearchSnapshotSqlProvider.class, method = "buildSearchSql")
    List<ProductSearchSnapshot> searchForProducts(@Param("query") ProductSearchQuery query);

    @SelectProvider(type = ProductSearchSnapshotSqlProvider.class, method = "buildCountSql")
    long countForSearch(@Param("query") ProductSearchQuery query);
}

package com.example.demo.demos.search.mapper;

import com.example.demo.demos.search.entity.SearchCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 搜索类目 Mapper — 对应 category 表。
 */
@Mapper
public interface SearchCategoryMapper {

    @Insert("INSERT INTO category (category_id, parent_category_id, category_name, category_level, " +
            "category_path, is_leaf, sort_order, status) " +
            "VALUES (#{categoryId}, #{parentCategoryId}, #{categoryName}, #{categoryLevel}, " +
            "#{categoryPath}, #{isLeaf}, #{sortOrder}, #{status})")
    int insert(SearchCategory category);

    @Select("SELECT category_id, parent_category_id, category_name, category_level, " +
            "category_path, is_leaf, sort_order, status, created_at, updated_at " +
            "FROM category WHERE category_id = #{categoryId}")
    SearchCategory selectById(@Param("categoryId") Long categoryId);

    @Select("SELECT category_id, parent_category_id, category_name, category_level, " +
            "category_path, is_leaf, sort_order, status, created_at, updated_at " +
            "FROM category WHERE parent_category_id = #{parentCategoryId} AND status = 'enabled' " +
            "ORDER BY sort_order")
    List<SearchCategory> selectByParentId(@Param("parentCategoryId") Long parentCategoryId);

    @Select("SELECT category_id, parent_category_id, category_name, category_level, " +
            "category_path, is_leaf, sort_order, status, created_at, updated_at " +
            "FROM category WHERE status = 'enabled' ORDER BY category_level, sort_order")
    List<SearchCategory> selectAllEnabled();

    @Select("SELECT category_id, parent_category_id, category_name, category_level, " +
            "category_path, is_leaf, sort_order, status, created_at, updated_at " +
            "FROM category WHERE category_name = #{categoryName} AND status = 'enabled'")
    List<SearchCategory> selectByName(@Param("categoryName") String categoryName);

    @Update("UPDATE category SET category_name = #{categoryName}, parent_category_id = #{parentCategoryId}, " +
            "category_level = #{categoryLevel}, category_path = #{categoryPath}, " +
            "is_leaf = #{isLeaf}, sort_order = #{sortOrder}, status = #{status} " +
            "WHERE category_id = #{categoryId}")
    int update(SearchCategory category);
}

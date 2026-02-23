package com.example.demo.demos.CommunityMarket.Dao;

import com.example.demo.demos.CommunityMarket.Pojo.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM categories")
    List<Category> getAllCategories();

    @Select("SELECT * FROM categories WHERE parent_id = #{parentId}")
    List<Category> getAllCategoriesByParentId(String parentId);
}

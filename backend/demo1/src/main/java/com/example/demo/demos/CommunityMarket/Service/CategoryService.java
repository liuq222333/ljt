package com.example.demo.demos.CommunityMarket.Service;

import com.example.demo.demos.CommunityMarket.Pojo.Category;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();

    List<Category> getAllCategoriesByParentId(String parentId);
}

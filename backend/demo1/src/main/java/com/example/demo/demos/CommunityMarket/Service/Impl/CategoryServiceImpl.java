package com.example.demo.demos.CommunityMarket.Service.Impl;

import com.example.demo.demos.CommunityMarket.Dao.CategoryMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Category;
import com.example.demo.demos.CommunityMarket.Service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.getAllCategories();
    }

    @Override
    public List<Category> getAllCategoriesByParentId(String parentId) {
        List<Category> list = categoryMapper.getAllCategoriesByParentId(parentId);
        return list;
    }
}

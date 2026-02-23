package com.example.demo.demos.CommunityMarket.Controller;

import com.example.demo.demos.CommunityMarket.Pojo.Category;
import com.example.demo.demos.CommunityMarket.Service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    CategoryService categoryService;

    @Operation(summary = "获取所有分类")
    @RequestMapping("/getAllCategories")
    public List<Category> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @Operation(summary = "获取分类下的所有子类")
    @RequestMapping("/getAllCategories/parentId/{parentId}")
    public List<Category> getAllCategoriesByParentId(@PathVariable String parentId) {
        return categoryService.getAllCategoriesByParentId(parentId);
    }

}

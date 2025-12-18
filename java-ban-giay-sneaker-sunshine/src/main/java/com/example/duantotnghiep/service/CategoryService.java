package com.example.duantotnghiep.service;

import com.example.duantotnghiep.dto.request.CategoryRequest;
import com.example.duantotnghiep.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
    CategoryResponse getCategoryById(Long id);
}

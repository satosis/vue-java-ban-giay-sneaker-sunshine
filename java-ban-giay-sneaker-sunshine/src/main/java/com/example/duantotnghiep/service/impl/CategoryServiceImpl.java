package com.example.duantotnghiep.service.impl;

import com.example.duantotnghiep.dto.request.CategoryRequest;
import com.example.duantotnghiep.dto.response.CategoryResponse;
import com.example.duantotnghiep.mapper.CategoryMapper;
import com.example.duantotnghiep.model.Category;
import com.example.duantotnghiep.repository.CategoryRepository;
import com.example.duantotnghiep.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findByStatus().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        category.setCategoryCode(generateCode());
        category.setCreatedBy("admin");
        category.setCreatedDate(new Date());
        category.setStatus(1);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(null);
        existingCategory.setUpdatedBy("admin");
        existingCategory.setUpdatedDate(new Date());
        categoryMapper.updateEntity(existingCategory, request);
        return categoryMapper.toResponse(categoryRepository.save(existingCategory));
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(null);
        category.setStatus(0); // Soft delete
        categoryRepository.save(category);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(null);
        return categoryMapper.toResponse(category);
    }

    private String generateCode() {
        String prefix = "CATEGORY-";
        String datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + datePart + "-" + randomPart;
    }
}

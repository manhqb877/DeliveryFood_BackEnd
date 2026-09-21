package com.fooddelivery.core.service;

import com.fooddelivery.core.dto.request.CategoryRequest;
import com.fooddelivery.core.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategoriesByShopId(Long shopId);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}

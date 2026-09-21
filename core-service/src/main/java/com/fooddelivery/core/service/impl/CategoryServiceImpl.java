package com.fooddelivery.core.service.impl;

import com.fooddelivery.core.dto.request.CategoryRequest;
import com.fooddelivery.core.dto.response.CategoryResponse;
import com.fooddelivery.core.entity.Category;
import com.fooddelivery.core.entity.Shop;
import com.fooddelivery.core.exception.ResourceNotFoundException;
import com.fooddelivery.core.repository.CategoryRepository;
import com.fooddelivery.core.repository.ShopRepository;
import com.fooddelivery.core.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<CategoryResponse> getCategoriesByShopId(Long shopId) {
        log.info("Fetching categories for shopId: {}", shopId);
        List<Category> categories = categoryRepository.findByShopIdAndIsActiveTrueOrderBySortOrderAsc(shopId);
        return categories.stream()
                .map(cat -> modelMapper.map(cat, CategoryResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category for shopId: {}", request.getShopId());
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + request.getShopId()));

        Category category = modelMapper.map(request, Category.class);
        category.setShop(shop);
        if (request.getIsActive() == null) {
            category.setIsActive(true);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created with id: {}", savedCategory.getId());
        return modelMapper.map(savedCategory, CategoryResponse.class);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // Note: shop mapping not usually updated, but we could if request changes shopId.
        // Assuming shopId doesn't change for a category.
        
        modelMapper.map(request, category);
        category.setId(id); // prevent override

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated with id: {}", updatedCategory.getId());
        return modelMapper.map(updatedCategory, CategoryResponse.class);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted with id: {}", id);
    }
}

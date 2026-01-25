package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateCategoryRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateCategoryRequest;
import com.restaurant.qrorder.domain.dto.response.CategoryResponse;
import com.restaurant.qrorder.domain.entity.Category;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.CategoryMapper;
import com.restaurant.qrorder.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.debug("Creating new category: {}", request.getName());

        categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .ifPresent(cat -> {
                    throw new DuplicateResourceException("Category already exists with name: " + request.getName());
                });

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", savedCategory.getId());

        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        log.debug("Updating category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            categoryRepository.findAllByOrderByNameAsc().stream()
                    .filter(cat -> cat.getName().equalsIgnoreCase(request.getName()) && !cat.getId().equals(id))
                    .findFirst()
                    .ifPresent(cat -> {
                        throw new DuplicateResourceException("Category already exists with name: " + request.getName());
                    });
            category.setName(request.getName());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", id);

        return categoryMapper.toResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.debug("Deleting category with id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }
}

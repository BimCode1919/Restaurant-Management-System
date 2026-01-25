package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateCategoryRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateCategoryRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.CategoryResponse;
import com.restaurant.qrorder.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Category Management", description = "APIs for managing categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieve all menu categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(
                ApiResponse.<List<CategoryResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Categories retrieved successfully")
                        .data(categoryService.getAllCategories())
                        .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieve a specific category by its ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Category retrieved successfully")
                        .data(categoryService.getCategoryById(id))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new category", description = "Create a new menu category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CategoryResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Category created successfully")
                        .data(categoryService.createCategory(request))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category", description = "Update an existing category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Category updated successfully")
                        .data(categoryService.updateCategory(id, request))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category", description = "Delete a category by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Category deleted successfully")
                        .build());
    }
}

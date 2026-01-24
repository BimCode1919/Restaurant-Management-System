package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.CategoryResponse;
import com.restaurant.qrorder.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}

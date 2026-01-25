package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateItemRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Item Management", description = "APIs for managing menu items")
@SecurityRequirement(name = "bearerAuth")
public class ItemController {

    ItemService itemService;

    @GetMapping
    @Operation(summary = "Get all items", description = "Retrieve all menu items with pagination")
    public ResponseEntity<ApiResponse<Page<ItemResponse>>> getAllItems(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(
                ApiResponse.<Page<ItemResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Items retrieved successfully")
                        .data(itemService.getAllItems(categoryId, pageable))
                        .build());
    }

    @GetMapping("/available")
    @Operation(summary = "Get available items", description = "Retrieve all available menu items with optional category filter")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getAvailableItems(
            @RequestParam(required = false) Long categoryId
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<ItemResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Available items retrieved successfully")
                        .data(itemService.getAvailableItems(categoryId))
                        .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID", description = "Retrieve a specific item by its ID")
    public ResponseEntity<ApiResponse<ItemResponse>> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ItemResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Item retrieved successfully")
                        .data(itemService.getItemById(id))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new item", description = "Create a new menu item (Admin only)")
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(@Valid @RequestBody CreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<ItemResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Item created successfully")
                        .data(itemService.createItem(request))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete item", description = "Delete a menu item by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Item deleted successfully")
                        .build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search items", description = "Search items by name or description")
    public ResponseEntity<ApiResponse<Page<ItemResponse>>> searchItems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                ApiResponse.<Page<ItemResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Items found successfully")
                        .data(itemService.searchItems(keyword, pageable))
                        .build());
    }
}

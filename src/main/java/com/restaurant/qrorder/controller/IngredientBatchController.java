package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateIngredientBatchRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.IngredientBatchResponse;
import com.restaurant.qrorder.service.IngredientBatchService;
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
@RequestMapping("/batches")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Ingredient Batch Management", description = "APIs for managing ingredient batches")
@SecurityRequirement(name = "bearerAuth")
public class IngredientBatchController {

    IngredientBatchService batchService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all batches", description = "Retrieve all ingredient batches (Admin only)")
    public ResponseEntity<ApiResponse<List<IngredientBatchResponse>>> getAllBatches() {
        return ResponseEntity.ok(
                ApiResponse.<List<IngredientBatchResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Batches retrieved successfully")
                        .data(batchService.getAllBatches())
                        .build());
    }

    @GetMapping("/ingredient/{ingredientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get batches by ingredient", description = "Retrieve all batches for a specific ingredient (Admin only)")
    public ResponseEntity<ApiResponse<List<IngredientBatchResponse>>> getBatchesByIngredient(@PathVariable Long ingredientId) {
        return ResponseEntity.ok(
                ApiResponse.<List<IngredientBatchResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Batches retrieved successfully")
                        .data(batchService.getBatchesByIngredient(ingredientId))
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get batch by ID", description = "Retrieve a specific batch by its ID (Admin only)")
    public ResponseEntity<ApiResponse<IngredientBatchResponse>> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<IngredientBatchResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Batch retrieved successfully")
                        .data(batchService.getBatchById(id))
                        .build());
    }

    @GetMapping("/expired")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get expired batches", description = "Retrieve all expired batches (Admin only)")
    public ResponseEntity<ApiResponse<List<IngredientBatchResponse>>> getExpiredBatches() {
        return ResponseEntity.ok(
                ApiResponse.<List<IngredientBatchResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Expired batches retrieved successfully")
                        .data(batchService.getExpiredBatches())
                        .build());
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get expiring batches", description = "Retrieve batches expiring within specified days (Admin only)")
    public ResponseEntity<ApiResponse<List<IngredientBatchResponse>>> getExpiringBatches(
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(
                ApiResponse.<List<IngredientBatchResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Expiring batches retrieved successfully")
                        .data(batchService.getExpiringBatches(days))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new batch", description = "Create a new ingredient batch (Admin only)")
    public ResponseEntity<ApiResponse<IngredientBatchResponse>> createBatch(@Valid @RequestBody CreateIngredientBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<IngredientBatchResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Batch created successfully")
                        .data(batchService.createBatch(request))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete batch", description = "Delete an ingredient batch by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Batch deleted successfully")
                        .build());
    }
}

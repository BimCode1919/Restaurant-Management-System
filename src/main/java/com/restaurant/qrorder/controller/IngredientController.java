package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateIngredientRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateIngredientRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.IngredientResponse;
import com.restaurant.qrorder.service.IngredientService;
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
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Ingredient Management", description = "APIs for managing ingredients")
@SecurityRequirement(name = "bearerAuth")
public class IngredientController {

    IngredientService ingredientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF')")
    @Operation(summary = "Get all ingredients", description = "Retrieve all ingredients (Admin manages, Chef views)")
    public ResponseEntity<ApiResponse<List<IngredientResponse>>> getAllIngredients() {
        return ResponseEntity.ok(
                ApiResponse.<List<IngredientResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Ingredients retrieved successfully")
                        .data(ingredientService.getAllIngredients())
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF')")
    @Operation(summary = "Get ingredient by ID", description = "Retrieve a specific ingredient by its ID (Admin manages, Chef views)")
    public ResponseEntity<ApiResponse<IngredientResponse>> getIngredientById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<IngredientResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Ingredient retrieved successfully")
                        .data(ingredientService.getIngredientById(id))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new ingredient", description = "Create a new ingredient (Admin only)")
    public ResponseEntity<ApiResponse<IngredientResponse>> createIngredient(@Valid @RequestBody CreateIngredientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<IngredientResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Ingredient created successfully")
                        .data(ingredientService.createIngredient(request))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update ingredient", description = "Update an existing ingredient (Admin only)")
    public ResponseEntity<ApiResponse<IngredientResponse>> updateIngredient(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIngredientRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<IngredientResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Ingredient updated successfully")
                        .data(ingredientService.updateIngredient(id, request))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete ingredient", description = "Delete an ingredient by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Ingredient deleted successfully")
                        .build());
    }
}

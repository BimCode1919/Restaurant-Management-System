package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateDiscountRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.service.DiscountService;
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
@RequestMapping("/discounts")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Tag(name = "Discount Management", description = "APIs for managing discounts and promotions")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    DiscountService discountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all discounts", description = "Retrieve all discounts (Admin/Staff only)")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getAllDiscounts() {
        return ResponseEntity.ok(
                ApiResponse.<List<DiscountResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discounts retrieved successfully")
                        .data(discountService.getAllDiscounts())
                        .build());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active discounts", description = "Retrieve all currently active and valid discounts")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getActiveDiscounts() {
        return ResponseEntity.ok(
                ApiResponse.<List<DiscountResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Active discounts retrieved successfully")
                        .data(discountService.getActiveDiscounts())
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get discount by ID", description = "Retrieve a specific discount by its ID")
    public ResponseEntity<ApiResponse<DiscountResponse>> getDiscountById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount retrieved successfully")
                        .data(discountService.getDiscountById(id))
                        .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new discount", description = "Create a new discount (Admin only)")
    public ResponseEntity<ApiResponse<DiscountResponse>> createDiscount(@Valid @RequestBody CreateDiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Discount created successfully")
                        .data(discountService.createDiscount(request))
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update discount", description = "Update an existing discount (Admin only)")
    public ResponseEntity<ApiResponse<DiscountResponse>> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiscountRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount updated successfully")
                        .data(discountService.updateDiscount(id, request))
                        .build());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle discount status", description = "Activate or deactivate a discount (Admin only)")
    public ResponseEntity<ApiResponse<DiscountResponse>> toggleDiscountStatus(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount status toggled successfully")
                        .data(discountService.toggleDiscountStatus(id))
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete discount", description = "Delete a discount by ID (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount deleted successfully")
                        .build());
    }
}

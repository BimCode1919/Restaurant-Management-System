package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.ValidateDiscountRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.service.BillService;
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
    BillService billService;

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

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get discount by code", description = "Retrieve a specific discount by its code")
    public ResponseEntity<ApiResponse<DiscountResponse>> getDiscountByCode(@PathVariable String code) {
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount retrieved successfully")
                        .data(discountService.getDiscountByCode(code))
                        .build());
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate discount code", description = "Validate if a discount code is applicable")
    public ResponseEntity<ApiResponse<DiscountResponse>> validateDiscount(
            @RequestBody ValidateDiscountRequest request) {
        
        DiscountResponse discount = discountService.validateDiscountCode(
                request.getCode(), 
                request.getOrderAmount(), 
                request.getPartySize());
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount validated successfully")
                        .data(discount)
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

    @PostMapping("/calculate/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Calculate best discount for bill", 
               description = "Find and calculate the best applicable discount for a specific bill")
    public ResponseEntity<ApiResponse<DiscountResponse>> calculateBestDiscount(@PathVariable Long billId) {
        DiscountResponse bestDiscount = billService.findBestDiscount(billId);
        
        if (bestDiscount == null) {
            return ResponseEntity.ok(
                    ApiResponse.<DiscountResponse>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("No applicable discount found for this bill")
                            .build());
        }
        
        return ResponseEntity.ok(
                ApiResponse.<DiscountResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Best discount calculated successfully")
                        .data(bestDiscount)
                        .build());
    }

    @PostMapping("/apply/{billId}/{discountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Apply discount to bill", 
               description = "Apply a specific discount to a bill")
    public ResponseEntity<ApiResponse<Void>> applyDiscountToBill(
            @PathVariable Long billId,
            @PathVariable Long discountId) {
        
        billService.applyDiscount(billId, discountId);
        
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount applied successfully to bill")
                        .build());
    }

    @PostMapping("/apply-best/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Apply best discount to bill", 
               description = "Automatically find and apply the best discount to a bill")
    public ResponseEntity<ApiResponse<Void>> applyBestDiscount(@PathVariable Long billId) {
        billService.applyBestDiscount(billId);
        
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Best discount applied successfully")
                        .build());
    }

    @DeleteMapping("/remove/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Remove discount from bill", 
               description = "Remove the currently applied discount from a bill")
    public ResponseEntity<ApiResponse<Void>> removeDiscountFromBill(@PathVariable Long billId) {
        billService.removeDiscount(billId);
        
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount removed successfully from bill")
                        .build());
    }
}

package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.dto.request.CreateBillRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.BillResponse;
import com.restaurant.qrorder.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
@Tag(name = "Bill Management", description = "APIs for managing restaurant bills")
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    @Operation(summary = "Create bill", description = "Create a new bill for tables (Admin/Staff only)")
    public ResponseEntity<ApiResponse<BillResponse>> createBill(@Valid @RequestBody CreateBillRequest request) {
        BillResponse response = billService.createBill(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Bill created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get bill by ID", description = "Retrieve bill details")
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(@PathVariable Long id) {
        BillResponse response = billService.getBillResponseById(id);
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Bill retrieved successfully")
                        .data(response)
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get all bills", description = "Retrieve all bills or filter by status (Admin/Staff only)")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getAllBills(
            @RequestParam(required = false) BillStatus status) {
        
        List<BillResponse> responses = status != null 
                ? billService.getBillsByStatus(status)
                : billService.getAllBills();
        
        return ResponseEntity.ok(
                ApiResponse.<List<BillResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Bills retrieved successfully")
                        .data(responses)
                        .build());
    }

    @PostMapping("/{id}/apply-discount")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Apply discount to bill", description = "Apply a discount to the bill (Admin/Staff only)")
    public ResponseEntity<ApiResponse<BillResponse>> applyDiscount(
            @PathVariable Long id,
            @RequestParam Long discountId) {
        
        billService.applyDiscount(id, discountId);
        BillResponse response = billService.getBillResponseById(id);
        
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount applied successfully")
                        .data(response)
                        .build());
    }

    @PostMapping("/{id}/apply-best-discount")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Apply best discount", description = "Automatically apply the best available discount (Admin/Staff only)")
    public ResponseEntity<ApiResponse<BillResponse>> applyBestDiscount(@PathVariable Long id) {
        billService.applyBestDiscount(id);
        BillResponse response = billService.getBillResponseById(id);
        
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Best discount applied successfully")
                        .data(response)
                        .build());
    }

    @DeleteMapping("/{id}/discount")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Remove discount", description = "Remove discount from bill (Admin/Staff only)")
    public ResponseEntity<ApiResponse<BillResponse>> removeDiscount(@PathVariable Long id) {
        billService.removeDiscount(id);
        BillResponse response = billService.getBillResponseById(id);
        
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Discount removed successfully")
                        .data(response)
                        .build());
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Close bill", description = "Close a paid bill and release tables (Admin/Staff only)")
    public ResponseEntity<ApiResponse<BillResponse>> closeBill(@PathVariable Long id) {
        BillResponse response = billService.closeBill(id);
        return ResponseEntity.ok(
                ApiResponse.<BillResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Bill closed successfully")
                        .data(response)
                        .build());
    }
}

package com.restaurant.qrorder.controller;


import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.dto.response.OrderResponse;
import com.restaurant.qrorder.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order-details")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF', 'STAFF')")
    @Operation(summary = "Update item status", description = "Update the preparation status of a single order item")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody ItemStatus request) {

        OrderDetailResponse responses = orderDetailService.updateItemStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.<OrderDetailResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Update order status successfully")
                        .data(responses)
                        .build());
    }

    @GetMapping("/sortByOldest")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF', 'STAFF')")
    @Operation(summary = "Get a list of order detail sorted by oldest first")
    public ResponseEntity<ApiResponse<List<OrderDetailResponse>>> getOldestOrderFirst()
    {
        List<OrderDetailResponse> responses = orderDetailService.getAllOrderDetails();
        return ResponseEntity.ok(
                ApiResponse.<List<OrderDetailResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Fetched order details sorted by oldest first")
                        .data(responses)
                        .build());
    }

    @GetMapping("/ready")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF', 'STAFF')")
    @Operation(summary = "Get a list of order details with status READY, excluding SERVED")
    public ResponseEntity<ApiResponse<List<OrderDetailResponse>>> getReadyOrders() {
        List<OrderDetailResponse> responses = orderDetailService.getReadyOrderDetails();
        return ResponseEntity.ok(
                ApiResponse.<List<OrderDetailResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Fetched READY order details successfully")
                        .data(responses)
                        .build());
    }
}

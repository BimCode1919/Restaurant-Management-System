package com.restaurant.qrorder.controller;


import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order-details")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class OrderDetailController {
    private final OrderDetailService orderDetailService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF', 'STAFF')")
    @Operation(summary = "Update item status", description = "Update the preparation status of a single order item")
    public ResponseEntity<OrderDetailResponse> updateItemStatus(
            @PathVariable Long id,
            @Valid @RequestBody ItemStatus request) {

        return ResponseEntity.ok(orderDetailService.updateItemStatus(id, request));
    }
}

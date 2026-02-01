package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.CreateOrderRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.OrderResponse;
import com.restaurant.qrorder.service.OrderService;
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
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing food orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Create order", description = "Create a new food order for a bill")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<OrderResponse>builder()
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Order created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Get order by ID", description = "Retrieve order details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(
                ApiResponse.<OrderResponse>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Order retrieved successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @Operation(summary = "Get orders by bill", description = "Retrieve all orders for a specific bill")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByBillId(@PathVariable Long billId) {
        List<OrderResponse> responses = orderService.getOrdersByBillId(billId);
        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponse>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Orders retrieved successfully")
                        .data(responses)
                        .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Delete order", description = "Cancel/delete an order (Admin/Staff only)")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .statusCode(HttpStatus.OK.value())
                        .message("Order deleted successfully")
                        .build());
    }
}

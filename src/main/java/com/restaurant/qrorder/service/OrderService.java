package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.OrderType;
import com.restaurant.qrorder.domain.dto.request.CreateOrderRequest;
import com.restaurant.qrorder.domain.dto.request.OrderDetailRequest;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.dto.response.OrderResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.ItemRepository;
import com.restaurant.qrorder.repository.OrderRepository;
import com.restaurant.qrorder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final BillService billService;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    /**
     * Create new order
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for bill {}", request.getBillId());
        
        // Get bill and validate
        Bill bill = billService.getBillById(request.getBillId());
        
        if (bill.getStatus() != com.restaurant.qrorder.domain.common.BillStatus.OPEN) {
            throw new InvalidOperationException("Cannot add order to a non-open bill");
        }
        
        // Get current user
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Create order
        Order order = Order.builder()
                .bill(bill)
                .orderType(request.getOrderType() != null ? request.getOrderType() : OrderType.AT_TABLE)
                .createdBy(user)
                .orderDetails(new ArrayList<>())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        // Create order details
        for (OrderDetailRequest itemRequest : request.getItems()) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemRequest.getItemId()));
            
            if (!item.getAvailable()) {
                throw new InvalidOperationException("Item " + item.getName() + " is not available");
            }
            
            OrderDetail detail = OrderDetail.builder()
                    .order(savedOrder)
                    .item(item)
                    .quantity(itemRequest.getQuantity())
                    .price(item.getPrice())
                    .note(itemRequest.getNotes())
                    .build();
            
            savedOrder.getOrderDetails().add(detail);
        }
        
        orderRepository.save(savedOrder);
        
        // Recalculate bill totals
        billService.recalculateBill(bill.getId());
        
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return mapToResponse(savedOrder);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToResponse(order);
    }

    /**
     * Get all orders for a bill
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBillId(Long billId) {
        Bill bill = billService.getBillById(billId);
        return bill.getOrders().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete order (only if bill is still open)
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        log.info("Deleting order {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        
        Bill bill = order.getBill();
        
        if (bill.getStatus() != com.restaurant.qrorder.domain.common.BillStatus.OPEN) {
            throw new InvalidOperationException("Cannot delete order from a non-open bill");
        }
        
        orderRepository.delete(order);
        
        // Recalculate bill totals
        billService.recalculateBill(bill.getId());
        
        log.info("Order {} deleted successfully", orderId);
    }

    /**
     * Map Order entity to OrderResponse DTO
     */
    private OrderResponse mapToResponse(Order order) {
        BigDecimal totalAmount = order.getOrderDetails().stream()
                .map(detail -> detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return OrderResponse.builder()
                .id(order.getId())
                .billId(order.getBill().getId())
                .orderType(order.getOrderType())
                .createdBy(order.getCreatedBy().getFullName())
                .totalAmount(totalAmount)
                .items(order.getOrderDetails().stream()
                        .map(this::mapOrderDetailToResponse)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderDetailResponse mapOrderDetailToResponse(OrderDetail detail) {
        BigDecimal subtotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        
        return OrderDetailResponse.builder()
                .id(detail.getId())
                .itemId(detail.getItem().getId())
                .itemName(detail.getItem().getName())
                .quantity(detail.getQuantity())
                .price(detail.getPrice())
                .subtotal(subtotal)
                .notes(detail.getNote())
                .build();
    }
}

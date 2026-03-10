package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.common.OrderType;
import com.restaurant.qrorder.domain.common.UserRole;
import com.restaurant.qrorder.domain.dto.request.CreateOrderRequest;
import com.restaurant.qrorder.domain.dto.request.OrderDetailRequest;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.dto.response.OrderResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final RoleRepository roleRepository;
    private final OrderDetailRepository orderDetailRepository;

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = userRepository.findByEmail("guest@qr.local")
                    .orElseGet(() -> {
                        User guest = User.builder()
                                .email("guest@qr.local")
                                .fullName("QR Guest")
                                .password("")
                                .role(roleRepository.findByName(UserRole.CUSTOMER).orElseThrow())
                                .build();
                        return userRepository.save(guest);
                    });
        }

        String roleName = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("CUSTOMER");

        ItemStatus itemStatus = null;
        if ("ROLE_STAFF".equalsIgnoreCase(roleName) || "STAFF".equalsIgnoreCase(roleName)) {
            itemStatus = ItemStatus.PREPARING;
        } else {
            itemStatus = ItemStatus.PENDING;
        }

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

            if (itemStatus == null) {
                throw new RuntimeException("Item status is null!!!");
            }

            OrderDetail detail = OrderDetail.builder()
                    .order(savedOrder)
                    .item(item)
                    .quantity(itemRequest.getQuantity())
                    .price(item.getPrice())
                    .note(itemRequest.getNotes())
                    .itemStatus(itemStatus)
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
    @Transactional
    public List<OrderDetailResponse> massUpdateOrderItemStatus(Long orderId)
    {
        log.info("Mass update order items with orderId: {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        List<OrderDetail> orderDetailList = order.getOrderDetails();
        if(orderDetailList.isEmpty()) {
            throw new InvalidOperationException("Can't update an empty order");
        }
        List<OrderDetailResponse> orderDetailResponseList = new ArrayList<>();
        for(OrderDetail orderDetail : orderDetailList)
        {
            if (orderDetail.getItemStatus() == ItemStatus.CANCELLED) {
                continue;
            }
            orderDetail.setItemStatus(ItemStatus.PREPARING);
         orderDetailResponseList.add(mapOrderDetailToResponse(orderDetailRepository.save(orderDetail)));
            log.info("Successfully updated order detail with id:", orderDetail.getId());
        }
        return orderDetailResponseList;
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
                .itemStatus(detail.getItemStatus())
                .notes(detail.getNote())
                .build();
    }
}

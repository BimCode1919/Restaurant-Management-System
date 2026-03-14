package com.restaurant.qrorder.service;


import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.entity.Bill;
import com.restaurant.qrorder.domain.entity.Order;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.mapper.ItemMapper;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.OrderDetailRepository;
import com.restaurant.qrorder.util.OrderDetailSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private  Specification<OrderDetail> specification;
    private final BillRepository billRepository;
    @Transactional
    public OrderDetailResponse updateItemStatus(Long orderDetailId, ItemStatus request) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "OrderDetail not found with id: " + orderDetailId));

        ItemStatus oldStatus = orderDetail.getItemStatus(); // ← capture BEFORE mutating

        if (request == ItemStatus.CANCELLED) {
            Bill bill = orderDetail.getOrder().getBill();
            bill.setTotalPrice(bill.getTotalPrice().subtract(
                    orderDetail.getPrice().multiply(BigDecimal.valueOf(orderDetail.getQuantity()))
            ));
            // Recalculate finalPrice so discount stays consistent
            bill.setFinalPrice(bill.getTotalPrice().subtract(bill.getDiscountAmount()));
            billRepository.save(bill);
        }

        orderDetail.setItemStatus(request);
        OrderDetail saved = orderDetailRepository.save(orderDetail);

        log.info("OrderDetail ID: {} status updated: {} → {}", saved.getId(), oldStatus, request);

        return mapToResponse(saved);
    }

    private OrderDetailResponse mapToResponse(OrderDetail od) {
        return OrderDetailResponse.builder()
                .id(od.getId())
                .itemId(od.getItem().getId())
                .itemName(od.getItem().getName())
                .quantity(od.getQuantity())
                .price(od.getPrice())
                .subtotal(od.getPrice().multiply(BigDecimal.valueOf(od.getQuantity())))
                .itemStatus(od.getItemStatus())
                .notes(od.getNote())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrderDetailsByStatus(ItemStatus status) {
        Specification<OrderDetail> spec = new OrderDetailSpecification().getItemByStatus(status); // ← local variable
        return orderDetailRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }



}

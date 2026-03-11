package com.restaurant.qrorder.service;


import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.mapper.ItemMapper;
import com.restaurant.qrorder.repository.OrderDetailRepository;
import com.restaurant.qrorder.util.OrderDetailSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private  Specification<OrderDetail> specification;
    @Transactional
    public OrderDetailResponse updateItemStatus(Long orderDetailId, ItemStatus request) {

        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "OrderDetail not found with id: " + orderDetailId));

//        validateTransition(orderDetail.getItemStatus(), request.getItemStatus());

        orderDetail.setItemStatus(request);


        OrderDetail saved = orderDetailRepository.save(orderDetail);
        log.info("OrderDetail ID: {} status updated: {} → {}",
                saved.getId(), orderDetail.getItemStatus(), request);

        return mapToResponse(saved);
    }

    private OrderDetailResponse mapToResponse(OrderDetail od) {
        return OrderDetailResponse.builder()
                .id(od.getId())
                .itemId(od.getItem().getId())
                .itemName(od.getItem().getName())
                .quantity(od.getQuantity())
                .itemStatus(od.getItemStatus())
                .notes(od.getNote())
                .price(od.getPrice())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrderDetailsByStatus(ItemStatus status) {
              specification = new OrderDetailSpecification().getItemByStatus(status);
        return orderDetailRepository.findAll(specification).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }



}

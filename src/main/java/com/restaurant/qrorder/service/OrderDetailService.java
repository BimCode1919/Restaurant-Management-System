package com.restaurant.qrorder.service;


import com.restaurant.qrorder.repository.OrderDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {
    private final OrderDetailRepository orderDetailRepository;


}

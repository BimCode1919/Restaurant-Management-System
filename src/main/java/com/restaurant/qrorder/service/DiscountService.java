package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateDiscountRequest;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.DiscountMapper;
import com.restaurant.qrorder.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DiscountService {

    DiscountRepository discountRepository;
    DiscountMapper discountMapper;

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        log.debug("Fetching all discounts");
        return discountRepository.findAll().stream()
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getActiveDiscounts() {
        log.debug("Fetching active discounts");
        return discountRepository.findActiveDiscounts(LocalDateTime.now()).stream()
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long id) {
        log.debug("Fetching discount by id: {}", id);
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
        return discountMapper.toResponse(discount);
    }

    @Transactional
    public DiscountResponse createDiscount(CreateDiscountRequest request) {
        log.debug("Creating new discount: {}", request.getName());

        Discount discount = discountMapper.toEntity(request);
        Discount savedDiscount = discountRepository.save(discount);

        log.info("Discount created successfully with id: {}", savedDiscount.getId());
        return discountMapper.toResponse(savedDiscount);
    }

    @Transactional
    public DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        log.debug("Updating discount with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        if (request.getName() != null) {
            discount.setName(request.getName());
        }

        if (request.getDescription() != null) {
            discount.setDescription(request.getDescription());
        }

        if (request.getDiscountType() != null) {
            discount.setDiscountType(request.getDiscountType());
        }

        if (request.getValue() != null) {
            discount.setValue(request.getValue());
        }

        if (request.getMinOrderAmount() != null) {
            discount.setMinOrderAmount(request.getMinOrderAmount());
        }

        if (request.getMaxDiscountAmount() != null) {
            discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }

        if (request.getStartDate() != null) {
            discount.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            discount.setEndDate(request.getEndDate());
        }

        if (request.getUsageLimit() != null) {
            discount.setUsageLimit(request.getUsageLimit());
        }

        // Update advanced fields
        if (request.getMinPartySize() != null) {
            discount.setMinPartySize(request.getMinPartySize());
        }

        if (request.getMaxPartySize() != null) {
            discount.setMaxPartySize(request.getMaxPartySize());
        }

        if (request.getTierConfig() != null) {
            discount.setTierConfig(request.getTierConfig());
        }

        if (request.getApplicableDays() != null) {
            discount.setApplicableDays(request.getApplicableDays());
        }

        if (request.getApplyToSpecificItems() != null) {
            discount.setApplyToSpecificItems(request.getApplyToSpecificItems());
        }

        if (request.getActive() != null) {
            discount.setActive(request.getActive());
        }

        Discount updatedDiscount = discountRepository.save(discount);
        log.info("Discount updated successfully with id: {}", id);

        return discountMapper.toResponse(updatedDiscount);
    }

    @Transactional
    public void deleteDiscount(Long id) {
        log.debug("Deleting discount with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        discountRepository.delete(discount);
        log.info("Discount deleted successfully with id: {}", id);
    }

    @Transactional
    public DiscountResponse toggleDiscountStatus(Long id) {
        log.debug("Toggling discount status with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        discount.setActive(!discount.getActive());
        Discount updatedDiscount = discountRepository.save(discount);

        log.info("Discount status toggled to {} for id: {}", updatedDiscount.getActive(), id);
        return discountMapper.toResponse(updatedDiscount);
    }
}

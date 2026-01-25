package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateItemRequest;
import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.domain.entity.Category;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.ItemMapper;
import com.restaurant.qrorder.repository.CategoryRepository;
import com.restaurant.qrorder.repository.DiscountRepository;
import com.restaurant.qrorder.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ItemService {

    ItemRepository itemRepository;
    CategoryRepository categoryRepository;
    DiscountRepository discountRepository;
    ItemMapper itemMapper;

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAllItems(Long categoryId, Pageable pageable) {
        log.debug("Fetching all items with pagination. CategoryId: {}", categoryId);
        
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            
            return itemRepository.findByCategoryId(categoryId, pageable)
                    .map(this::mapToResponseWithDiscounts);
        }
        
        return itemRepository.findAll(pageable)
                .map(this::mapToResponseWithDiscounts);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAvailableItems(Long categoryId) {
        log.debug("Fetching all available items. CategoryId: {}", categoryId);
        
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            
            return itemRepository.findAvailableItemsByCategory(categoryId).stream()
                    .map(this::mapToResponseWithDiscounts)
                    .collect(Collectors.toList());
        }
        
        return itemRepository.findAllAvailableOrderByName().stream()
                .map(this::mapToResponseWithDiscounts)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        log.debug("Fetching item by id: {}", id);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return mapToResponseWithDiscounts(item);
    }

    @Transactional
    public ItemResponse createItem(CreateItemRequest request) {
        log.debug("Creating new item: {}", request.getName());
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Item item = itemMapper.toEntity(request);
        item.setCategory(category);
        
        Item savedItem = itemRepository.save(item);
        log.info("Item created successfully with id: {}", savedItem.getId());
        
        return mapToResponseWithDiscounts(savedItem);
    }

    @Transactional
    public void deleteItem(Long id) {
        log.debug("Deleting item with id: {}", id);
        
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        
        itemRepository.delete(item);
        log.info("Item deleted successfully with id: {}", id);
    }
    
    private ItemResponse mapToResponseWithDiscounts(Item item) {
        ItemResponse response = itemMapper.toResponse(item);
        
        List<ItemResponse.ActiveDiscountInfo> activeDiscounts = item.getDiscounts().stream()
                .filter(discount -> discount.getDiscountType() == DiscountType.ITEM_SPECIFIC)
                .filter(this::isDiscountActive)
                .map(discount -> ItemResponse.ActiveDiscountInfo.builder()
                        .discountId(discount.getId())
                        .discountName(discount.getName())
                        .discountValue(discount.getValue())
                        .discountType(discount.getDiscountType().name())
                        .build())
                .collect(Collectors.toList());
        
        response.setActiveDiscounts(activeDiscounts);
        
        return response;
    }
    
    private boolean isDiscountActive(Discount discount) {
        if (!discount.getActive()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            return false;
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            return false;
        }
        
        if (discount.getDiscountType() == DiscountType.ITEM_SPECIFIC && discount.getApplicableDays() != null && !discount.getApplicableDays().isEmpty()) {
            String[] dayStrings = discount.getApplicableDays().split(",");
            DayOfWeek today = now.getDayOfWeek();
            
            boolean isDayApplicable = false;
            for (String dayStr : dayStrings) {
                try {
                    DayOfWeek day = DayOfWeek.valueOf(dayStr.trim().toUpperCase());
                    if (day == today) {
                        isDayApplicable = true;
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid day of week: {}", dayStr);
                }
            }
            
            if (!isDayApplicable) {
                return false;
            }
        }
        
        return true;
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> searchItems(String keyword, Pageable pageable) {
        log.debug("Searching items with keyword: {}", keyword);
        return itemRepository.searchItems(keyword, pageable)
                .map(this::mapToResponseWithDiscounts);
    }
}


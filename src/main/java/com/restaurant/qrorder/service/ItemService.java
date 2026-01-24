package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.ItemMapper;
import com.restaurant.qrorder.repository.CategoryRepository;
import com.restaurant.qrorder.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    ItemMapper itemMapper;

    @Transactional(readOnly = true)
    public Page<ItemResponse> getAllItems(Long categoryId, Pageable pageable) {
        log.debug("Fetching all items with pagination. CategoryId: {}", categoryId);
        
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            
            return itemRepository.findByCategoryId(categoryId, pageable)
                    .map(itemMapper::toResponse);
        }
        
        return itemRepository.findAll(pageable)
                .map(itemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAvailableItems(Long categoryId) {
        log.debug("Fetching all available items. CategoryId: {}", categoryId);
        
        if (categoryId != null) {
            categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
            
            return itemRepository.findAvailableItemsByCategory(categoryId).stream()
                    .map(itemMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        return itemRepository.findAllAvailableOrderByName().stream()
                .map(itemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        log.debug("Fetching item by id: {}", id);
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return itemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> searchItems(String keyword, Pageable pageable) {
        log.debug("Searching items with keyword: {}", keyword);
        return itemRepository.searchItems(keyword, pageable)
                .map(itemMapper::toResponse);
    }
}


package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateIngredientBatchRequest;
import com.restaurant.qrorder.domain.dto.response.IngredientBatchResponse;
import com.restaurant.qrorder.domain.entity.Ingredient;
import com.restaurant.qrorder.domain.entity.IngredientBatch;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.IngredientBatchMapper;
import com.restaurant.qrorder.repository.IngredientBatchRepository;
import com.restaurant.qrorder.repository.IngredientRepository;
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
public class IngredientBatchService {

    IngredientBatchRepository batchRepository;
    IngredientRepository ingredientRepository;
    IngredientBatchMapper batchMapper;

    @Transactional(readOnly = true)
    public List<IngredientBatchResponse> getAllBatches() {
        log.debug("Fetching all ingredient batches");
        return batchRepository.findAll().stream()
                .map(batchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientBatchResponse> getBatchesByIngredient(Long ingredientId) {
        log.debug("Fetching batches for ingredient id: {}", ingredientId);
        
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new ResourceNotFoundException("Ingredient not found with id: " + ingredientId);
        }

        return batchRepository.findByIngredientIdOrderByCreatedAtDesc(ingredientId).stream()
                .map(batchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IngredientBatchResponse getBatchById(Long id) {
        log.debug("Fetching ingredient batch by id: {}", id);
        IngredientBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient batch not found with id: " + id));
        return batchMapper.toResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<IngredientBatchResponse> getExpiredBatches() {
        log.debug("Fetching expired batches");
        return batchRepository.findExpiredBatches(LocalDateTime.now()).stream()
                .map(batchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IngredientBatchResponse> getExpiringBatches(int days) {
        log.debug("Fetching batches expiring in {} days", days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(days);
        
        return batchRepository.findExpiringBatches(now, future).stream()
                .map(batchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public IngredientBatchResponse createBatch(CreateIngredientBatchRequest request) {
        log.debug("Creating new ingredient batch for ingredient id: {}", request.getIngredientId());
        
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + request.getIngredientId()));

        IngredientBatch batch = IngredientBatch.builder()
                .ingredient(ingredient)
                .quantity(request.getQuantity())
                .expiryDate(request.getExpiryDate())
                .build();

        ingredient.setStockQuantity(ingredient.getStockQuantity().add(request.getQuantity()));
        ingredientRepository.save(ingredient);

        IngredientBatch savedBatch = batchRepository.save(batch);
        log.info("Ingredient batch created successfully with id: {}", savedBatch.getId());
        
        return batchMapper.toResponse(savedBatch);
    }

    @Transactional
    public void deleteBatch(Long id) {
        log.debug("Deleting ingredient batch with id: {}", id);
        
        IngredientBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient batch not found with id: " + id));
        
        Ingredient ingredient = batch.getIngredient();
        ingredient.setStockQuantity(ingredient.getStockQuantity().subtract(batch.getQuantity()));
        ingredientRepository.save(ingredient);

        batchRepository.delete(batch);
        log.info("Ingredient batch deleted successfully with id: {}", id);
    }
}

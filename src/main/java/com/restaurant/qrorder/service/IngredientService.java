package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateIngredientRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateIngredientRequest;
import com.restaurant.qrorder.domain.dto.response.IngredientResponse;
import com.restaurant.qrorder.domain.entity.Ingredient;
import com.restaurant.qrorder.exception.custom.DuplicateResourceException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.IngredientMapper;
import com.restaurant.qrorder.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class IngredientService {

    IngredientRepository ingredientRepository;
    IngredientMapper ingredientMapper;

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAllIngredients() {
        log.debug("Fetching all ingredients");
        return ingredientRepository.findAllByOrderByNameAsc().stream()
                .map(ingredientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredientById(Long id) {
        log.debug("Fetching ingredient by id: {}", id);
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));
        return ingredientMapper.toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse createIngredient(CreateIngredientRequest request) {
        log.debug("Creating new ingredient: {}", request.getName());
        
        if (ingredientRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Ingredient already exists with name: " + request.getName());
        }

        Ingredient ingredient = ingredientMapper.toEntity(request);
        Ingredient savedIngredient = ingredientRepository.save(ingredient);
        
        log.info("Ingredient created successfully with id: {}", savedIngredient.getId());
        return ingredientMapper.toResponse(savedIngredient);
    }

    @Transactional
    public IngredientResponse updateIngredient(Long id, UpdateIngredientRequest request) {
        log.debug("Updating ingredient with id: {}", id);
        
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));

        if (request.getName() != null && !request.getName().equals(ingredient.getName())) {
            if (ingredientRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Ingredient already exists with name: " + request.getName());
            }
            ingredient.setName(request.getName());
        }

        if (request.getStockQuantity() != null) {
            ingredient.setStockQuantity(request.getStockQuantity());
        }

        if (request.getUnit() != null) {
            ingredient.setUnit(request.getUnit());
        }

        Ingredient updatedIngredient = ingredientRepository.save(ingredient);
        log.info("Ingredient updated successfully with id: {}", id);
        
        return ingredientMapper.toResponse(updatedIngredient);
    }

    @Transactional
    public void deleteIngredient(Long id) {
        log.debug("Deleting ingredient with id: {}", id);
        
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found with id: " + id));
        
        ingredientRepository.delete(ingredient);
        log.info("Ingredient deleted successfully with id: {}", id);
    }
}

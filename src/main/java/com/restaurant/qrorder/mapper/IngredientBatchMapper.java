package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.response.IngredientBatchResponse;
import com.restaurant.qrorder.domain.entity.IngredientBatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IngredientBatchMapper {

    @Mapping(source = "ingredient.id", target = "ingredientId")
    @Mapping(source = "ingredient.name", target = "ingredientName")
    IngredientBatchResponse toResponse(IngredientBatch batch);
}

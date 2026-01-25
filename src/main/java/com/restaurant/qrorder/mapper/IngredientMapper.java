package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.request.CreateIngredientRequest;
import com.restaurant.qrorder.domain.dto.response.IngredientResponse;
import com.restaurant.qrorder.domain.entity.Ingredient;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    IngredientResponse toResponse(Ingredient ingredient);

    Ingredient toEntity(CreateIngredientRequest request);
}

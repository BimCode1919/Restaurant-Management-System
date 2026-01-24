package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.response.CategoryResponse;
import com.restaurant.qrorder.domain.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}

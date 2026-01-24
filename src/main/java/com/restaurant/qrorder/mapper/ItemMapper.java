package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.domain.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "category.name", target = "categoryName")
    ItemResponse toResponse(Item item);
}

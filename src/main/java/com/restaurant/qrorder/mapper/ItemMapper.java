package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.request.CreateItemRequest;
import com.restaurant.qrorder.domain.dto.response.ItemResponse;
import com.restaurant.qrorder.domain.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(source = "category.name", target = "categoryName")
    ItemResponse toResponse(Item item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "recipes", ignore = true)
    @Mapping(target = "orderDetails", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Item toEntity(CreateItemRequest request);
}

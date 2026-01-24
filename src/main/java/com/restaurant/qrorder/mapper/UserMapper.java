package com.restaurant.qrorder.mapper;

import com.restaurant.qrorder.domain.dto.response.UserResponse;
import com.restaurant.qrorder.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.name", target = "role")
    UserResponse toResponse(User user);
}

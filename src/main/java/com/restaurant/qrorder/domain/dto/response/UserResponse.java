package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UserResponse {

    Long id;
    String fullName;
    String email;
    String phone;
    UserRole role;
    Boolean active;
    LocalDateTime createdAt;
}

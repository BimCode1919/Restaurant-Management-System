package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Full name must contain only letters, spaces, or - . '")
    private String fullName;

    @Email(message = "Email must be a valid format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s\\-().]{7,20}$", message = "Phone number format is invalid")
    private String phone;
}
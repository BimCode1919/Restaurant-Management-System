package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Full name must contain only letters, spaces, or - . '")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)"
    )
    private String password;

    @Pattern(regexp = "^\\+?[0-9\\s\\-().]{7,20}$", message = "Phone number format is invalid")
    private String phone;

    @NotNull(message = "Role ID is required")
    @Positive(message = "Role ID must be a positive number")
    private Long roleId;
}
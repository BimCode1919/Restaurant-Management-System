package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid phone number format")
    private String customerPhone;

    @Email(message = "Invalid email format")
    private String customerEmail;

    @NotNull(message = "Party size is required")
    @Min(value = 1, message = "Party size must be at least 1")
    @Max(value = 50, message = "Party size cannot exceed 50")
    private Integer partySize;

    @NotNull(message = "Reservation time is required")
    @Future(message = "Reservation time must be in the future")
    private LocalDateTime reservationTime;

    private String note;


    // Requested table IDs (optional)
    @NotEmpty(message = "Table need to not be fill ín !!!!")
    @Valid
    private List<Long> requestedTableIds;

    // Pre-order items (optional)
    private List<PreOrderItemRequest> preOrderItems;

    @NotNull(message = "Reservation start time is required")
    private LocalTime startTime;

    @NotNull(message = "Reservation end time is required")
    private LocalTime endTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreOrderItemRequest {
        @NotNull(message = "Item ID is required")
        private Long itemId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        private String note;
    }
}

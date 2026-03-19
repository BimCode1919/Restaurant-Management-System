package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified reservation request DTO.
 *
 * Deposit is determined automatically by the service:
 *   - partySize > 10      → deposit required
 *   - preOrderItems present → deposit required
 *   - otherwise             → no deposit
 *
 * requestedTableIds is optional; omit/null to let the system auto-assign.
 */
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

    // Optional: explicit table selection. If null/empty the system auto-assigns.
    @Valid
    private List<Long> requestedTableIds;

    // Optional: pre-order items. Presence triggers deposit requirement.
    @Valid
    private List<PreOrderItemRequest> preOrderItems;

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

package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    
    @NotNull(message = "Table ID is required")
    private Long tableId;
    
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;
    
    private Integer partySize;
    
    private String note;
    
    @Data
    public static class OrderItemRequest {
        @NotNull(message = "Item ID is required")
        private Long itemId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private String note;
    }
}

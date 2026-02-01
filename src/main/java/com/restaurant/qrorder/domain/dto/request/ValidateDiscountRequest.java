package com.restaurant.qrorder.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateDiscountRequest {
    private String code;
    private Double orderAmount;
    private Integer partySize;
}

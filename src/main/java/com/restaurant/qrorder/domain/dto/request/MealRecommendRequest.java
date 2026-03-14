package com.restaurant.qrorder.domain.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MealRecommendRequest {

    private String prompt;

    private BigDecimal budget;

    private Integer people;

    private Preference preferences;

}

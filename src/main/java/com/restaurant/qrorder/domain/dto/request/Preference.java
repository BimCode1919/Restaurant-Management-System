package com.restaurant.qrorder.domain.dto.request;

import lombok.Data;

@Data
public class Preference {

    private Boolean spicy;

    private Boolean vegetarian;

    private String mealType;
}

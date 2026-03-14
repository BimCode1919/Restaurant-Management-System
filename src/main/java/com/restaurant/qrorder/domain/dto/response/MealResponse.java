package com.restaurant.qrorder.domain.dto.response;

import lombok.Data;

@Data
public class MealResponse {
    private String appetizer;
    private String mainCourse;
    private String dessert;
}

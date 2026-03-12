package com.restaurant.qrorder.exception.custom;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientIngredientException  extends RuntimeException {
    private final List<String> details;

    public InsufficientIngredientException (String message) {
        super(message);
        // Parse each line into a list for structured API response
        this.details = Arrays.asList(message.split("\n"));
    }

    public List<String> getDetails() {
        return details;
    }
}

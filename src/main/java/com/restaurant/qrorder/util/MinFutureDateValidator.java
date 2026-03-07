package com.restaurant.qrorder.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.restaurant.qrorder.util.MinFutureDate;
import java.time.LocalDateTime;

public class MinFutureDateValidator implements ConstraintValidator<MinFutureDate, LocalDateTime> {
    private int minDays;

    @Override
    public void initialize(MinFutureDate annotation) {
        this.minDays = annotation.minDays();
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull handles the null case

        LocalDateTime minAllowedDate = LocalDateTime.now().plusDays(minDays);
        return !value.isBefore(minAllowedDate);
    }
}

package com.restaurant.qrorder.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinFutureDateValidator.class)
public @interface MinFutureDate {  // ← @interface at top level, capital M
    int minDays() default 30;
    String message() default "Date must be at least {minDays} days in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
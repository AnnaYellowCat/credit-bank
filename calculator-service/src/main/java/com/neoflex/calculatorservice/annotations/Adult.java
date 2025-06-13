package com.neoflex.calculatorservice.annotations;

import com.neoflex.calculatorservice.validators.AgeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AgeValidator.class)
public @interface Adult {
    String message() default "Age must be at least 18";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
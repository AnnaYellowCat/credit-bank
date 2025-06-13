package com.neoflex.calculatorservice.validators;

import com.neoflex.calculatorservice.annotations.Adult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class AgeValidator implements ConstraintValidator<Adult, LocalDate> {
    final int ADULT_AGE = 18;
    @Override
    public boolean isValid(LocalDate birthdate, ConstraintValidatorContext context) {
        Period age = Period.between(birthdate, LocalDate.now());
        return age.getYears() >= ADULT_AGE;
    }
}

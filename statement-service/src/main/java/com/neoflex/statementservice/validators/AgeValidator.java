package com.neoflex.statementservice.validators;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class AgeValidator {
    private static final int ADULT_AGE = 18;

    public boolean isAdult(LocalDate birthDate) {
        if (birthDate == null) {
            return false;
        }
        Period age = Period.between(birthDate, LocalDate.now());
        return age.getYears() >= ADULT_AGE;
    }
}
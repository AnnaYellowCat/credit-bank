package com.neoflex.statementservice.validators;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AgeValidatorTests {
    @Autowired
    private AgeValidator ageValidator;

    @Test
    public void isAdult_ReturnsTrue_WhenAgeIsAbove18() {
        boolean result = ageValidator.isAdult(LocalDate.parse("2000-03-01"));

        assertTrue(result);
    }

    @Test
    public void isAdult_ReturnsFalse_WhenAgeIsUnder18() {
        boolean result = ageValidator.isAdult(LocalDate.parse("2020-03-01"));

        assertFalse(result);
    }

    @Test
    public void isAdult_ReturnsFalse_WhenBirthDateIsNull() {
        boolean result = ageValidator.isAdult(null);

        assertFalse(result);
    }
}

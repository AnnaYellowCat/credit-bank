package com.neoflex.calculatorservice.dto;

import com.neoflex.calculatorservice.annotations.Adult;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LoanStatementRequestDto {
    @NotNull
    @Min(10000)
    @Max(100000000)
    private BigDecimal amount;

    @NotNull
    @Min(3)
    @Max(360)
    private Integer term;

    @NotNull
    @Pattern(regexp = "^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    private String firstName;

    @NotNull
    @Pattern(regexp = "^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    private String lastName;

    @Pattern(regexp = "^$|^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    private String middleName;

    @NotNull
    @Pattern(regexp = "^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$")
    private String email;

    @NotNull
    @Past
    @Adult
    private LocalDate birthdate;

    @NotNull
    @Pattern(regexp = "^[0-9]{4}$")
    private String passportSeries;

    @NotNull
    @Pattern(regexp = "^[0-9]{6}$")
    private String passportNumber;
}

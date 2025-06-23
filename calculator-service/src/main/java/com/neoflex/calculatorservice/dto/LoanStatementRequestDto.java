package com.neoflex.calculatorservice.dto;

import com.neoflex.calculatorservice.annotations.Adult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Data for creating loan offers", name = "LoanStatementRequestDto")
public class LoanStatementRequestDto {
    @NotNull
    @Min(10000)
    @Max(100000000)
    @Schema(description = "Requested loan amount, ", example = "200000")
    private BigDecimal amount;

    @NotNull
    @Min(3)
    @Max(360)
    @Schema(description = "Loan term, number of months", example = "24")
    private Integer term;

    @NotNull
    @Pattern(regexp = "^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    @Schema(description = "First name of client", example = "Ivan")
    private String firstName;

    @NotNull
    @Pattern(regexp = "^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    @Schema(description = "Last name of client", example = "Ivanov")
    private String lastName;

    @Pattern(regexp = "^$|^[a-zA-Zа-яёА-ЯЁ-]{1,60}$")
    @Schema(description = "Middle name of client", example = "Ivanovich")
    private String middleName;

    @NotNull
    @Pattern(regexp = "^[a-z0-9A-Z_!#$%&'*+/=?`{|}~^.-]+@[a-z0-9A-Z.-]+$")
    @Schema(description = "Email of client", example = "ivan@mail.ru")
    private String email;

    @NotNull
    @Past
    @Adult
    @Schema(description = "Date of birth of client", example = "2000-01-01")
    private LocalDate birthDate;

    @NotNull
    @Pattern(regexp = "^[0-9]{4}$")
    @Schema(description = "Passport series of client", example = "6060")
    private String passportSeries;

    @NotNull
    @Pattern(regexp = "^[0-9]{6}$")
    @Schema(description = "Passport number of client", example = "473058")
    private String passportNumber;
}

package com.neoflex.calculatorservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Data for creating loan offers", name = "LoanStatementRequestDto")
public class LoanStatementRequestDto {
    @Schema(description = "Requested loan amount, ", example = "200000")
    private BigDecimal amount;

    @Schema(description = "Loan term, number of months", example = "24")
    private Integer term;

    @Schema(description = "First name of client", example = "Ivan")
    private String firstName;

    @Schema(description = "Last name of client", example = "Ivanov")
    private String lastName;

    @Schema(description = "Middle name of client", example = "Ivanovich")
    private String middleName;

    @Schema(description = "Email of client", example = "ivan@mail.ru")
    private String email;

    @Schema(description = "Date of birth of client", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "Passport series of client", example = "6060")
    private String passportSeries;

    @Schema(description = "Passport number of client", example = "473058")
    private String passportNumber;
}

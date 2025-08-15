package com.neoflex.calculatorservice.dto;

import com.neoflex.calculatorservice.enums.Gender;
import com.neoflex.calculatorservice.enums.MaritalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data for calculating credit data", name = "ScoringDataDto")
public class ScoringDataDto {
    @Schema(description = "Requested loan amount", example = "200000")
    private BigDecimal amount;

    @Schema(description = "Loan term, number of months", example = "24")
    private Integer term;

    @Schema(description = "First name of client", example = "Ivan")
    private String firstName;

    @Schema(description = "Last name of client", example = "Ivanov")
    private String lastName;

    @Schema(description = "Middle name of client", example = "Ivanovich")
    private String middleName;

    @Schema(description = "Gender of client",
            allowableValues = {"MALE, FEMALE, NON_BINARY"},
            example = "MALE")
    private Gender gender;

    @Schema(description = "Date of birth of client", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "Passport series of client", example = "6060")
    private String passportSeries;

    @Schema(description = "Passport number of client", example = "473058")
    private String passportNumber;

    @Schema(description = "Date of issue of the client's passport", example = "2021-01-15")
    private LocalDate passportIssueDate;

    @Schema(description = "Client's passport issuing department",
            example = "Passport issuance department of the federal migration service for the saratov region")
    private String passportIssueBranch;

    @Schema(description = "Marital status of client",
            allowableValues = {"MARRIED, SINGLE, DIVORCED, WIDOW_WIDOWER"},
            example = "SINGLE")
    private MaritalStatus maritalStatus;

    @Schema(description = "Number of client's dependents", example = "0")
    private Integer dependentAmount;

    @Schema(description = "Detailed information about employment",
            example = """
                 {
                     "employmentStatus": "EMPLOYED",
                     "employerINN": "520205004556",
                     "salary": 40000,
                     "position": "WORKER",
                     "workExperienceTotal": 36,
                     "workExperienceCurrent": 24
                 }
            """)
    private EmploymentDto employment;

    @Schema(description = "Client's account number", example = "3567890")
    private String accountNumber;

    @Schema(description = "Information about whether insurance is included", example = "true")
    private Boolean isInsuranceEnabled;

    @Schema(description = "Information about whether the client is a salary client", example = "false")
    private Boolean isSalaryClient;
}

package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Calculated credit data", name = "CreditDto")
public class CreditDto {
    @Schema(description = "Requested loan amount", example = "200000")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal amount;

    @Schema(description = "Loan term, number of months", example = "24")
    private Integer term;

    @Schema(description = "The amount of payment for every month", example = "10000")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal monthlyPayment;

    @Schema(description = "Loan rate calculated depending on parameters, percentages", example = "22")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal rate;

    @Schema(description = "Total cost of credit", example = "250000")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal psk;

    @Schema(description = "Information about whether insurance is included", example = "true")
    private Boolean isInsuranceEnabled;

    @Schema(description = "Information about whether the client is a salary client", example = "false")
    private Boolean isSalaryClient;

    @Schema(description = "Payment schedule in the form of a list of elements with info about each payment",
            example = """
                 [
                     {
                         "number": 1,
                         "date": "2025-01-01",
                         "totalPayment": 10500,
                         "interestPayment": 500,
                         "debtPayment": 10000,
                         "remainingDebt": 190000
                     },
                     {
                         "number": 2,
                         "date": "2025-02-01",
                         "totalPayment": 10400,
                         "interestPayment": 490,
                         "debtPayment": 10000,
                         "remainingDebt": 180000
                     }
                 ]
            """)
    private List<PaymentScheduleElementDto> paymentSchedule;
}

package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Loan offer data", name = "LoanOfferDto")
public class LoanOfferDto {
      @Schema(description = "Unique id of loan statement", name = "647s37fb890d03s499g8")
      private UUID statementId;

      @Schema(description = "Requested loan amount", example = "200000")
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal requestedAmount;

      @Schema(description = "Total loan amount", example = "250000")
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal totalAmount;

      @Schema(description = "Loan term, number of months", example = "24")
      private Integer term;

      @Schema(description = "The amount of payment for every month", example = "10000")
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal monthlyPayment;

      @Schema(description = "Loan rate calculated depending on parameters, percentages", example = "22")
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal rate;

      @Schema(description = "Information about whether insurance is included", example = "true")
      private Boolean isInsuranceEnabled;

      @Schema(description = "Information about whether the client is a salary client", example = "false")
      private Boolean isSalaryClient;
}

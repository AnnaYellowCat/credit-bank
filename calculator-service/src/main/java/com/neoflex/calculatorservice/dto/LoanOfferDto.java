package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
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
public class LoanOfferDto {
      private UUID statementId;
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal requestedAmount;
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal totalAmount;
      private Integer term;
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal monthlyPayment;
      @JsonSerialize(using = BigDecimalSerializer.class)
      private BigDecimal rate;
      private Boolean isInsuranceEnabled;
      private Boolean isSalaryClient;
}

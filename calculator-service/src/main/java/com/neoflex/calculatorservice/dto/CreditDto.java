package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
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
public class CreditDto {
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal amount;
    private Integer term;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal monthlyPayment;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal rate;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal psk;
    private Boolean isInsuranceEnabled;
    private Boolean isSalaryClient;
    private List<PaymentScheduleElementDto> paymentSchedule;
}

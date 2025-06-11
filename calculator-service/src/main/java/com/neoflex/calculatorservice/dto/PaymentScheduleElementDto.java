package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentScheduleElementDto {
    private Integer number;
    private LocalDate date;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal totalPayment;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal interestPayment;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal debtPayment;
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal remainingDebt;
}

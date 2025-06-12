package com.neoflex.calculatorservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neoflex.calculatorservice.serializers.BigDecimalSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Info about one loan payment", name = "PaymentScheduleElementDto")
public class PaymentScheduleElementDto {
    @Schema(description = "Payment number", name = "4")
    private Integer number;

    @Schema(description = "Date of payment", name = "2025-04-18")
    private LocalDate date;

    @Schema(description = "Total payment amount", example = "10500")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal totalPayment;

    @Schema(description = "Interest part of the payment", example = "500")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal interestPayment;

    @Schema(description = "Main part of the payment", example = "10000")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal debtPayment;

    @Schema(description = "Remaining debt after payment", example = "160000")
    @JsonSerialize(using = BigDecimalSerializer.class)
    private BigDecimal remainingDebt;
}

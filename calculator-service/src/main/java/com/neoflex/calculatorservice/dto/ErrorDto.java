package com.neoflex.calculatorservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Credit denial info", name = "ErrorDto")
public class ErrorDto {
    @Schema(description = "Credit denial reason", example = "Age is more than 70")
    private String denialReason;
}
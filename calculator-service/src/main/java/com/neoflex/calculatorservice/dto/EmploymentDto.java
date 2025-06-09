package com.neoflex.calculatorservice.dto;

import com.neoflex.calculatorservice.enums.EmploymentStatus;
import com.neoflex.calculatorservice.enums.Position;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "Detailed information about employment", name = "EmploymentDto")
public class EmploymentDto {
    @Schema(description = "Employment status of client",
            allowableValues = {"UNEMPLOYED", "SELF_EMPLOYED", "EMPLOYED", "BUSINESS_OWNER"},
            example = "EMPLOYED")
    private EmploymentStatus employmentStatus;

    @Schema(description = "Taxpayer identification number of client's employer", example = "520205004556")
    private String employerINN;

    @Schema(description = "Size of client's salary", example = "40000")
    private BigDecimal salary;

    @Schema(description = "Position at work",
            allowableValues = {"WORKER, MIDDLE_MANAGER, TOP_MANAGER, OWNER"},
            example = "WORKER")
    private Position position;

    @Schema(description = "Total work experience, number of months", example = "36")
    private Integer workExperienceTotal;

    @Schema(description = "Current work experience, number of months", example = "24")
    private Integer workExperienceCurrent;
}

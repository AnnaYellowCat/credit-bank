package com.neoflex.calculatorservice.dto;

import com.neoflex.calculatorservice.enums.EmploymentStatus;
import com.neoflex.calculatorservice.enums.Position;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmploymentDto {
    private EmploymentStatus employmentStatus;
    private String employerINN;
    private BigDecimal salary;
    private Position position;
    private Integer workExperienceTotal;
    private Integer workExperienceCurrent;
}

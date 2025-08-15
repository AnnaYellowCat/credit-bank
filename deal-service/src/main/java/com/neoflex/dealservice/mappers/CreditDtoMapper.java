package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.CreditDto;
import com.neoflex.dealservice.entities.Credit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CreditDtoMapper {
    @Mapping(target = "isInsuranceEnabled", source = "insuranceEnabled")
    @Mapping(target = "isSalaryClient", source = "salaryClient")
    CreditDto toCreditDto(Credit credit);
}

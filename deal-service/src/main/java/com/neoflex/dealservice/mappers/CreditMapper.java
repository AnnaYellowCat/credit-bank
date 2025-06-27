package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.CreditDto;
import com.neoflex.dealservice.entities.Credit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring", imports = UUID.class)
public interface CreditMapper {

    @Mapping(target = "creditId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "insuranceEnabled", source = "isInsuranceEnabled")
    @Mapping(target = "salaryClient", source = "isSalaryClient")
    @Mapping(target = "creditStatus", constant = "CALCULATED")
    Credit toCredit(CreditDto creditDto);
}

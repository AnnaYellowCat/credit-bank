package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.CreditDto;
import com.neoflex.dealservice.entities.Credit;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.UUID;

import static com.neoflex.dealservice.enums.CreditStatus.CALCULATED;

@Mapper(componentModel = "spring")
public interface CreditMapper {

    @Mapping(target = "creditId", ignore = true)
    @Mapping(target = "insuranceEnabled", source = "isInsuranceEnabled")
    @Mapping(target = "salaryClient", source = "isSalaryClient")
    @Mapping(target = "creditStatus", ignore = true)
    Credit toCredit(CreditDto creditDto);

    @AfterMapping
    default void setRemainingFields(@MappingTarget Credit credit) {
        credit.setCreditId(UUID.randomUUID());
        credit.setCreditStatus(CALCULATED);
    }
}

package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.EmploymentDto;
import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.ScoringDataDto;
import com.neoflex.dealservice.entities.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ScoringDataDtoMapper {

    ScoringDataDto toScoringDto(Client client, LoanOfferDto offer, EmploymentDto employment);
}

package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.EmploymentDto;
import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.ScoringDataDto;
import com.neoflex.dealservice.entities.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScoringDataDtoMapper {

    @Mapping(target = "amount", source = "offer.requestedAmount")
    ScoringDataDto toScoringDto(Client client, LoanOfferDto offer, EmploymentDto employment);
}

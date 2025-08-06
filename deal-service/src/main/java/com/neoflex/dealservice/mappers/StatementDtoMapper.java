package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.StatementDto;
import com.neoflex.dealservice.entities.Statement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StatementDtoMapper {
    @Mapping(target = "clientId", expression = "java(statement.getClient().getClientId().toString())")
    @Mapping(target = "creditId", expression = "java(statement.getCredit()!=null?String.valueOf(statement.getCredit().getCreditId()):null)")
    StatementDto toStatementDto(Statement statement);
}

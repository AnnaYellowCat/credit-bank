package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Statement;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.ApplicationStatus.CC_APPROVED;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

@Mapper(componentModel = "spring")
public interface StatementMapper {

    @Mapping(target = "statementId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    @Mapping(target = "sesCode", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    Statement toStatement(Client client);

    Statement updateStatement(boolean creditDenied, Statement statement);

    @AfterMapping
    default void setRemainingFields(@MappingTarget Statement statement) {
        LocalDateTime creationDate = LocalDateTime.now();
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        statusHistory.add(StatementStatusHistoryDto.builder()
                .status(PREAPPROVAL)
                .time(creationDate)
                .changeType(AUTOMATIC)
                .build());
        statement.setStatementId(UUID.randomUUID());
        statement.setStatus(PREAPPROVAL);
        statement.setCreationDate(creationDate);
        statement.setSesCode(UUID.randomUUID().toString());
        statement.setStatusHistory(statusHistory);
    }

    @AfterMapping
    default void updateRemainingFields(boolean creditDenied, @MappingTarget Statement statement) {
        StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                .time(LocalDateTime.now())
                .changeType(AUTOMATIC)
                .build();
        if (creditDenied) {
            statusHistoryElement.setStatus(CC_DENIED);
            statement.setStatus(CC_DENIED);
        } else {
            statusHistoryElement.setStatus(CC_APPROVED);
            statement.setStatus(CC_APPROVED);
        }
        statement.getStatusHistory().add(statusHistoryElement);
    }
}

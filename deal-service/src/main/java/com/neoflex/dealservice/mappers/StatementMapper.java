package com.neoflex.dealservice.mappers;

import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.enums.ApplicationStatus;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

@Mapper(componentModel = "spring", imports = {UUID.class, ApplicationStatus.class})
public interface StatementMapper {

    @Mapping(target = "statementId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "status", constant = "PREAPPROVAL")
    @Mapping(target = "creationDate", source = "creationDate")
    @Mapping(target = "statusHistory", source = "creationDate", qualifiedByName = "createStatusHistory")
    Statement toStatement(Client client, LocalDateTime creationDate);

    @Mapping(target = "status", expression = "java(creditDenied?ApplicationStatus.CC_DENIED:ApplicationStatus.CC_APPROVED)")
    @Mapping(target = "statusHistory", expression = "java(updateStatusHistory(creditDenied, statement))")
    Statement updateStatement(boolean creditDenied, Statement statement);

    @Named("createStatusHistory")
    default List<StatementStatusHistoryDto> createStatusHistory(LocalDateTime creationDate) {
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        statusHistory.add(StatementStatusHistoryDto.builder()
                .status(PREAPPROVAL)
                .time(creationDate)
                .changeType(AUTOMATIC)
                .build());
        return statusHistory;
    }

    @Named("updateStatusHistory")
    default List<StatementStatusHistoryDto> updateStatusHistory(boolean creditDenied, Statement statement) {
        StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                .status(creditDenied ? CC_DENIED : CC_APPROVED)
                .time(LocalDateTime.now())
                .changeType(AUTOMATIC)
                .build();
        List<StatementStatusHistoryDto> statusHistory = statement.getStatusHistory();
        statusHistory.add(statusHistoryElement);
        return statusHistory;
    }
}

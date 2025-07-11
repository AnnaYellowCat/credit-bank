package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.EmailMessage;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.enums.ApplicationStatus;
import com.neoflex.dealservice.enums.EmailMessageTheme;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.StatementRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

public abstract class StatementService {
    protected final StatementRepository statementRepository;
    protected final KafkaProducer kafkaProducer;

    protected StatementService(StatementRepository statementRepository, KafkaProducer kafkaProducer) {
        this.statementRepository = statementRepository;
        this.kafkaProducer = kafkaProducer;
    }

    protected void updateStatementStatus(Statement statement, ApplicationStatus statementStatus) {
        statement.setStatus(statementStatus);
        StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                .status(statementStatus)
                .time(LocalDateTime.now())
                .changeType(AUTOMATIC)
                .build();
        statement.getStatusHistory().add(statusHistoryElement);
    }

    protected void sendKafkaMessage(UUID statementId, String clientEmail, EmailMessageTheme theme,
                                    String emailText) {
        kafkaProducer.sendMessage(EmailMessage.builder()
                .address(clientEmail)
                .theme(theme)
                .statementId(statementId)
                .text(emailText)
                .build());
    }
}
package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.EmailMessage;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.enums.ApplicationStatus;
import com.neoflex.dealservice.enums.EmailMessageTheme;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.StatementRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

@RequiredArgsConstructor
public abstract class StatementService {
    protected final StatementRepository statementRepository;
    protected final KafkaProducer kafkaProducer;

    private final static String EMPTY_TEXT = "";

    protected StatementStatusHistoryDto getStatusHistoryElement(ApplicationStatus statementStatus) {
        return StatementStatusHistoryDto.builder()
                .status(statementStatus)
                .time(LocalDateTime.now())
                .changeType(AUTOMATIC)
                .build();
    }

    protected void sendKafkaMessage(UUID statementId, String clientEmail, EmailMessageTheme theme,
                                    String emailText, String topic) {
        kafkaProducer.sendMessage(EmailMessage.builder()
                        .address(clientEmail)
                        .theme(theme)
                        .statementId(statementId)
                        .text(emailText)
                        .build(),
                topic);
    }

    protected void sendKafkaMessage(UUID statementId, String clientEmail, EmailMessageTheme theme, String topic) {
        kafkaProducer.sendMessage(EmailMessage.builder()
                        .address(clientEmail)
                        .theme(theme)
                        .statementId(statementId)
                        .text(EMPTY_TEXT)
                        .build(),
                topic);
    }
}
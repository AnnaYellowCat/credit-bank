package com.neoflex.dealservice.services;

import com.neoflex.dealservice.entities.Credit;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.enums.ApplicationStatus;
import com.neoflex.dealservice.enums.EmailMessageTheme;
import com.neoflex.dealservice.exceptions.DocumentIsAlreadySignedException;
import com.neoflex.dealservice.exceptions.InvalidCodeException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.CreditRepository;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.CreditStatus.ISSUED;

@Slf4j
@Service
public class IssueCreditService extends StatementService {
    private final CreditRepository creditRepository;

    public IssueCreditService(StatementRepository statementRepository, KafkaProducer kafkaProducer,
                              CreditRepository creditRepository) {
        super(statementRepository, kafkaProducer);
        this.creditRepository = creditRepository;
    }

    @Value("${topic.credit-issued}")
    private String creditIssuedTopic;

    @Transactional
    public void issueCredit(String statementId, String code) {
        UUID id = UUID.fromString(statementId);
        Statement statement = statementRepository.getReferenceById(id);
        try {
            if (statement.getStatus().equals(DOCUMENT_SIGNED) ||
                    statement.getStatus().equals(ApplicationStatus.CREDIT_ISSUED)) {
                log.error("Document for statement with id {} is already signed", statementId);
                throw new DocumentIsAlreadySignedException("Document is already signed");
            }
            if (statement.getSesCode().equals(code)) {
                statement.setStatus(DOCUMENT_SIGNED);
                statement.getStatusHistory().add(getStatusHistoryElement(DOCUMENT_SIGNED));
            } else {
                log.error("Code {} for statement with id {} is incorrect", code, statementId);
                throw new InvalidCodeException("Code is incorrect");
            }
            log.debug("Statement with id {} found", statementId);
            statement.setSignDate(LocalDateTime.now());
            statementRepository.save(statement);
            log.debug("Statement with id {} updated, status: {}", statementId, statement.getStatus());
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
        statement.setStatus(CREDIT_ISSUED);
        statement.getStatusHistory().add(getStatusHistoryElement(ApplicationStatus.CREDIT_ISSUED));
        log.debug("Statement with id {} updated, status: {}", statementId, statement.getStatus());

        Credit credit = statement.getCredit();
        credit.setCreditStatus(ISSUED);
        creditRepository.save(credit);
        log.debug("Credit for statement with id {} updated, status: {}", statementId, credit.getCreditStatus());
        sendKafkaMessage(id, statement.getClient().getEmail(), EmailMessageTheme.CREDIT_ISSUED, creditIssuedTopic);
    }
}

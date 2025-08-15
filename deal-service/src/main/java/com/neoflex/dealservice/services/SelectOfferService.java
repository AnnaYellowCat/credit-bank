package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.enums.ApplicationStatus;
import com.neoflex.dealservice.exceptions.DocumentIsAlreadySignedException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.EmailMessageTheme.FINISH_REGISTRATION;

@Slf4j
@Service
public class SelectOfferService extends StatementService {
    @Value("${topic.finish-registration}")
    private String finishRegTopic;

    public SelectOfferService(StatementRepository statementRepository, KafkaProducer kafkaProducer) {
        super(statementRepository, kafkaProducer);
    }

    @Transactional
    public void selectOffer(LoanOfferDto offer) {
        UUID statementId = offer.getStatementId();
        Statement statement = statementRepository.getReferenceById(statementId);
        try {
            if (statement.getStatus().equals(DOCUMENT_SIGNED) ||
                    statement.getStatus().equals(ApplicationStatus.CREDIT_ISSUED)) {
                log.error("Document for statement with id {} is already signed", statementId);
                throw new DocumentIsAlreadySignedException("Document is already signed");
            }
            statement.setAppliedOffer(offer);
            log.debug("Statement with id {} found", statementId);
            statement.setStatus(APPROVED);
            statement.getStatusHistory().add(getStatusHistoryElement(APPROVED));
            statementRepository.save(statement);
            log.debug("Statement with id {} updated, status: {}", statementId, statement.getStatus());
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
        sendKafkaMessage(statementId, statement.getClient().getEmail(), FINISH_REGISTRATION, finishRegTopic);
    }
}

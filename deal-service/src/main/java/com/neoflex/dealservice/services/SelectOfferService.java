package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.neoflex.dealservice.enums.ApplicationStatus.APPROVED;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

@Slf4j
@Service
public class SelectOfferService {
    private final StatementRepository statementRepository;

    public SelectOfferService(StatementRepository statementRepository) {
        this.statementRepository = statementRepository;
    }

    @Transactional
    public void selectOffer(LoanOfferDto offer) {
        Statement statement = statementRepository.getReferenceById(offer.getStatementId());
        try {
            statement.setAppliedOffer(offer);
            log.debug("Statement with id {} found", offer.getStatementId());
            statement.setStatus(APPROVED);
            StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                    .status(APPROVED)
                    .time(LocalDateTime.now())
                    .changeType(AUTOMATIC)
                    .build();
            statement.getStatusHistory().add(statusHistoryElement);
            statementRepository.save(statement);
            log.debug("Statement with id {} updated", offer.getStatementId());
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", offer.getStatementId());
            throw new StatementNotFoundException("Statement not found");
        }
    }
}

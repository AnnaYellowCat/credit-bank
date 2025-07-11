package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.CreditDto;
import com.neoflex.dealservice.entities.Credit;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.CreditNotFoundException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.mappers.CreditDtoMapper;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class FetchCreditInfoService {
    private final StatementRepository statementRepository;
    private final CreditDtoMapper creditDtoMapper;

    public FetchCreditInfoService(StatementRepository statementRepository, CreditDtoMapper creditDtoMapper) {
        this.statementRepository = statementRepository;
        this.creditDtoMapper = creditDtoMapper;
    }

    public CreditDto getCreditInfo(String statementId) {
        Statement statement = statementRepository.getReferenceById(UUID.fromString(statementId));
        try {
            Credit credit = statement.getCredit();
            log.debug("Statement with id {} found", statementId);
            if (credit == null) {
                log.error("Credit for statement with id {} not found", statementId);
                throw new CreditNotFoundException("Credit not found");
            }
            return creditDtoMapper.toCreditDto(credit);
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
    }
}

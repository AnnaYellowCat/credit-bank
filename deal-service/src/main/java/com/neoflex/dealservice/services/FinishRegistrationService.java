package com.neoflex.dealservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neoflex.dealservice.dto.*;
import com.neoflex.dealservice.entities.*;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.mappers.ClientMapper;
import com.neoflex.dealservice.mappers.CreditMapper;
import com.neoflex.dealservice.mappers.ScoringDataDtoMapper;
import com.neoflex.dealservice.mappers.StatementMapper;
import com.neoflex.dealservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
public class FinishRegistrationService {
    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final CreditRepository creditRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ScoringDataDtoMapper scoringDataDtoMapper;
    private final ClientMapper clientMapper;
    private final CreditMapper creditMapper;
    private final StatementMapper statementMapper;

    public FinishRegistrationService(StatementRepository statementRepository, ClientRepository clientRepository,
                                     CreditRepository creditRepository, RestTemplate restTemplate, ObjectMapper objectMapper,
                                     ScoringDataDtoMapper scoringDataDtoMapper, ClientMapper clientMapper, CreditMapper creditMapper,
                                     StatementMapper statementMapper) {
        this.statementRepository = statementRepository;
        this.clientRepository = clientRepository;
        this.creditRepository = creditRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.scoringDataDtoMapper = scoringDataDtoMapper;
        this.clientMapper = clientMapper;
        this.creditMapper = creditMapper;
        this.statementMapper = statementMapper;
    }

    @Value("${app.calculator.credit}")
    private String urlGetCredit;

    @Transactional
    public void finishRegistration(FinishRegistrationRequestDto finishRegistrationRequest, String statementId) {
        Client client;
        LoanOfferDto offer;
        Statement statement = statementRepository.getReferenceById(UUID.fromString(statementId));
        try {
            client = statement.getClient();
            offer = statement.getAppliedOffer();
        } catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
        log.debug("Statement with id {} found", statementId);

        Passport passport = client.getPassport();
        clientRepository.save(clientMapper.updateClient(client, passport, finishRegistrationRequest));
        log.debug("Client {} {} updated according to info for completion of registration",
                client.getFirstName(), client.getLastName());

        CreditDto creditDto = null;
        try {
            ResponseEntity<?> response = restTemplate.exchange(
                    urlGetCredit,
                    HttpMethod.POST,
                    new HttpEntity<>(scoringDataDtoMapper.toScoringDto(client, offer, finishRegistrationRequest.getEmployment())),
                    new ParameterizedTypeReference<>() {
                    }
            );
            creditDto = objectMapper.convertValue(response.getBody(), CreditDto.class);
        } catch (HttpServerErrorException e) {
            handleHttpServerErrorException(e, statement);
            return;
        } catch (Exception e) {
            log.error("Failed to get credit from calculator service due to unexpected error: {}", e.getMessage());
            throw new CalculatorServiceException("Failed to get credit from calculator service due to unexpected error");
        }
        log.debug("Credit from calculator service received successfully");
        Credit credit = creditMapper.toCredit(creditDto);
        creditRepository.save(credit);
        statement.setCredit(credit);
        log.debug("Credit with amount {}, psk {} rate {} and term {} saved",
                credit.getAmount(), credit.getPsk(), credit.getRate(), credit.getTerm());

        statementRepository.save(statementMapper.updateStatement(false, statement));
        log.debug("Statement with id {} updated and saved", statement.getStatementId());
    }

    private void handleHttpServerErrorException(HttpServerErrorException exception, Statement statement) {
        if (exception.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            try {
                ErrorDto error = exception.getResponseBodyAs(ErrorDto.class);
                Client client = statement.getClient();
                log.debug("Credit for client {} {} denied, reason: {}", client.getFirstName(), client.getLastName(),
                        error.getDenialReason());

                statementRepository.save(statementMapper.updateStatement(true, statement));
            } catch (IllegalStateException e) {
                log.error("Failed to get credit from calculator service, status code 500");
                throw new CalculatorServiceException("Failed to get credit from calculator service");
            }
        } else {
            log.error("Failed to get credit from calculator service, status code {}", exception.getStatusCode());
            throw new CalculatorServiceException("Failed to get credit from calculator service");
        }
    }
}

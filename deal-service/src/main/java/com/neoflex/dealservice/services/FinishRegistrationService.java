package com.neoflex.dealservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neoflex.dealservice.dto.*;
import com.neoflex.dealservice.entities.*;
import com.neoflex.dealservice.enums.EmploymentPosition;
import com.neoflex.dealservice.enums.EmploymentStatus;
import com.neoflex.dealservice.enums.Gender;
import com.neoflex.dealservice.enums.MaritalStatus;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;
import static com.neoflex.dealservice.enums.CreditStatus.CALCULATED;

@Slf4j
@Service
public class FinishRegistrationService {
    private final StatementRepository statementRepository;
    private final ClientRepository clientRepository;
    private final CreditRepository creditRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FinishRegistrationService(StatementRepository statementRepository, ClientRepository clientRepository,
                                     CreditRepository creditRepository, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.statementRepository = statementRepository;
        this.clientRepository = clientRepository;
        this.creditRepository = creditRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.calculator.credit}")
    private String urlGetCredit;

    @Transactional
    public void finishRegistration(FinishRegistrationRequestDto finishRegistrationRequest, String statementId) {
        Client client;
        LoanOfferDto offer;
        Statement statement = statementRepository.getReferenceById(UUID.fromString(statementId));
        try{
            client = statement.getClient();
            offer = statement.getAppliedOffer();
        }
        catch (EntityNotFoundException | NullPointerException e) {
            log.error("Statement with id {} not found", statementId);
            throw new StatementNotFoundException("Statement not found");
        }
        log.info("Statement with id {} found", statementId);

        client = updateClient(client, finishRegistrationRequest);
        clientRepository.save(client);
        log.info("Client {} {} updated according to info for completion of registration",
                client.getFirstName(), client.getLastName());

        ScoringDataDto scoringDataDto = createScoringData(client, offer, finishRegistrationRequest.getEmployment());
        try {
            ResponseEntity<?> response = restTemplate.exchange(
                    urlGetCredit,
                    HttpMethod.POST,
                    new HttpEntity<>(scoringDataDto),
                    new ParameterizedTypeReference<>() {
                    }
            );

            CreditDto creditDto = objectMapper.convertValue(response.getBody(), CreditDto.class);
            Credit credit = createCredit(creditDto);
            credit = creditRepository.save(credit);
            statement.setCredit(credit);
            log.info("Credit with amount {}, psk {} rate {} and term {} saved",
                    credit.getAmount(), credit.getPsk(), credit.getRate(), credit.getTerm());

            statement = updateStatement(false, statement);
            statementRepository.save(statement);
        }
        catch (HttpServerErrorException e) {
            handleHttpServerErrorException(e, statement);
        }
        catch (Exception e) {
            log.error("Failed to get credit from calculator service due to unexpected error: {}", e.getMessage());
            throw new CalculatorServiceException("Failed to get credit from calculator service due to unexpected error");
        }
        log.info("Statement with id {} updated and saved", statement.getStatementId());
    }

    private Client updateClient(Client client, FinishRegistrationRequestDto finishRegistrationRequest) {
        client.setGender(Gender.valueOf(finishRegistrationRequest.getGender()));
        client.setMaritalStatus(MaritalStatus.valueOf(finishRegistrationRequest.getMaritalStatus()));
        client.setDependentAmount(finishRegistrationRequest.getDependentAmount());
        client.setAccountNumber(finishRegistrationRequest.getAccountNumber());
        Passport passport = updatePassport(client.getPassport(), finishRegistrationRequest);
        Employment employment = createEmployment(finishRegistrationRequest.getEmployment());
        client.setPassport(passport);
        client.setEmployment(employment);
        return client;
    }

    private Passport updatePassport(Passport passport, FinishRegistrationRequestDto finishRegistrationRequest) {
        passport.setIssueDate(finishRegistrationRequest.getPassportIssueDate());
        passport.setIssueBranch(finishRegistrationRequest.getPassportIssueBranch());
        return passport;
    }

    private Employment createEmployment(EmploymentDto employmentDto) {
        return Employment.builder()
                .employmentId(UUID.randomUUID())
                .employmentStatus(EmploymentStatus.valueOf(employmentDto.getEmploymentStatus()))
                .employerInn(employmentDto.getEmployerINN())
                .salary(employmentDto.getSalary())
                .position(EmploymentPosition.valueOf(employmentDto.getPosition()))
                .workExperienceTotal(employmentDto.getWorkExperienceTotal())
                .workExperienceCurrent(employmentDto.getWorkExperienceCurrent())
                .build();
    }

    private ScoringDataDto createScoringData(Client client, LoanOfferDto offer, EmploymentDto employment) {
        Passport passport = client.getPassport();
        return ScoringDataDto.builder()
                .amount(offer.getRequestedAmount())
                .term(offer.getTerm())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .middleName(client.getMiddleName())
                .gender(client.getGender())
                .birthDate(client.getBirthDate())
                .passportSeries(passport.getSeries())
                .passportNumber(passport.getNumber())
                .passportIssueDate(passport.getIssueDate())
                .passportIssueBranch(passport.getIssueBranch())
                .maritalStatus(client.getMaritalStatus())
                .dependentAmount(client.getDependentAmount())
                .employment(employment)
                .accountNumber(client.getAccountNumber())
                .isInsuranceEnabled(offer.getIsInsuranceEnabled())
                .isSalaryClient(offer.getIsSalaryClient())
                .build();
    }

    private Credit createCredit(CreditDto creditDto) {
        return Credit.builder()
                .creditId(UUID.randomUUID())
                .amount(creditDto.getAmount())
                .term(creditDto.getTerm())
                .monthlyPayment(creditDto.getMonthlyPayment())
                .rate(creditDto.getRate())
                .psk(creditDto.getPsk())
                .paymentSchedule(creditDto.getPaymentSchedule())
                .insuranceEnabled(creditDto.getIsInsuranceEnabled())
                .salaryClient(creditDto.getIsSalaryClient())
                .creditStatus(CALCULATED)
                .build();
    }

    private Statement updateStatement(boolean creditDenied, Statement statement) {
        StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                .time(LocalDateTime.now())
                .changeType(AUTOMATIC)
                .build();
        if(creditDenied) {
            statusHistoryElement.setStatus(CC_DENIED);
            statement.setStatus(CC_DENIED);
        }
        else{
            statusHistoryElement.setStatus(CC_APPROVED);
            statement.setStatus(CC_APPROVED);
        }
        statement.getStatusHistory().add(statusHistoryElement);
        return statement;
    }

    private void handleHttpServerErrorException(HttpServerErrorException exception, Statement statement) {
        if (exception.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            try{
                ErrorDto error = exception.getResponseBodyAs(ErrorDto.class);
                Client client = statement.getClient();
                log.info("Credit for client {} {} denied, reason: {}", client.getFirstName(), client.getLastName(), error.getDenialReason());

                statement = updateStatement(true, statement);
                statementRepository.save(statement);
            }
            catch (IllegalStateException e) {
                log.error("Failed to get credit from calculator service, status code 500");
                throw new CalculatorServiceException("Failed to get credit from calculator service");
            }
        }
        else{
            log.error("Failed to get credit from calculator service, status code {}", exception.getStatusCode());
            throw new CalculatorServiceException("Failed to get credit from calculator service");
        }
    }
}

package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Passport;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.repositories.ClientRepository;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.PREAPPROVAL;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;

@Slf4j
@Service
public class CreateStatementService {
    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final RestTemplate restTemplate;

    public CreateStatementService(ClientRepository clientRepository, StatementRepository statementRepository, RestTemplate restTemplate) {
        this.clientRepository = clientRepository;
        this.statementRepository = statementRepository;
        this.restTemplate = restTemplate;
    }

    @Value("${app.calculator.offers}")
    private String urlGetOffers;

    @Transactional
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto statementRequest) {
        Client client = createClient(statementRequest);
        clientRepository.save(client);
        log.info("Client {} {} created", client.getFirstName(), client.getLastName());

        Statement statement = createStatement(statementRequest, client);
        statementRepository.save(statement);
        log.info("Statement for client {} {} created", client.getFirstName(), client.getLastName());

        try{
            ResponseEntity<List<LoanOfferDto>> response = restTemplate.exchange(
                    urlGetOffers,
                    HttpMethod.POST,
                    new HttpEntity<>(statementRequest),
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<LoanOfferDto> loanOffers = response.getBody();
            log.info("Loan offers from calculator service got successfully");
            for(LoanOfferDto loanOfferDto : loanOffers) {
                loanOfferDto.setStatementId(statement.getStatementId());
            }
            return loanOffers;
        }
        catch (HttpServerErrorException e) {
            log.error("Failed to get loan offers from calculator service, status code {}", e.getStatusCode());
            throw new CalculatorServiceException("Failed to get credit from calculator service");
        }
        catch (Exception e) {
            log.error("Failed to get loan offers from calculator service due to unexpected error: {}", e.getMessage());
            throw new CalculatorServiceException("Failed to get credit from calculator service due to unexpected error");
        }
    }

    private Client createClient(LoanStatementRequestDto statementRequest) {
        Passport passport = Passport.builder()
                .passportId(UUID.randomUUID())
                .series(statementRequest.getPassportSeries())
                .number(statementRequest.getPassportNumber())
                .build();

        return Client.builder()
                .clientId(UUID.randomUUID())
                .firstName(statementRequest.getFirstName())
                .lastName(statementRequest.getLastName())
                .middleName(statementRequest.getMiddleName())
                .birthDate(statementRequest.getBirthDate())
                .email(statementRequest.getEmail())
                .passport(passport)
                .build();
    }

    private Statement createStatement(LoanStatementRequestDto statementRequest, Client client) {
        LocalDateTime creationDate = LocalDateTime.now();
        StatementStatusHistoryDto statusHistoryElement = StatementStatusHistoryDto.builder()
                .status(PREAPPROVAL)
                .time(creationDate)
                .changeType(AUTOMATIC)
                .build();

        return Statement.builder()
                .statementId(UUID.randomUUID())
                .client(client)
                .status(PREAPPROVAL)
                .creationDate(creationDate)
                .sesCode(UUID.randomUUID().toString())
                .statusHistory(List.of(statusHistoryElement))
                .build();
    }
}

package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.mappers.ClientMapper;
import com.neoflex.dealservice.mappers.StatementMapper;
import com.neoflex.dealservice.repositories.ClientRepository;
import com.neoflex.dealservice.repositories.StatementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateStatementService {
    private final ClientRepository clientRepository;
    private final StatementRepository statementRepository;
    private final RestTemplate restTemplate;
    private final ClientMapper clientMapper;
    private final StatementMapper statementMapper;

    @Value("${app.calculator.offers}")
    private String urlGetOffers;

    @Transactional
    public List<LoanOfferDto> getOffers(LoanStatementRequestDto statementRequest) {
        Client client = clientMapper.toClient(statementRequest);
        clientRepository.save(client);
        log.debug("Client {} {} created", client.getFirstName(), client.getLastName());

        Statement statement = statementMapper.toStatement(client, LocalDateTime.now());
        statementRepository.save(statement);
        log.debug("Statement for client {} {} created", client.getFirstName(), client.getLastName());

        List<LoanOfferDto> loanOffers;
        try {
            ResponseEntity<List<LoanOfferDto>> response = restTemplate.exchange(
                    urlGetOffers,
                    HttpMethod.POST,
                    new HttpEntity<>(statementRequest),
                    new ParameterizedTypeReference<>() {
                    }
            );
            loanOffers = response.getBody();
        } catch (HttpServerErrorException e) {
            log.error("Failed to get loan offers from calculator service, status code {}", e.getStatusCode());
            throw new CalculatorServiceException("Failed to get credit from calculator service");
        } catch (Exception e) {
            log.error("Failed to get loan offers from calculator service due to unexpected error: {}", e.getMessage());
            throw new CalculatorServiceException("Failed to get credit from calculator service due to unexpected error");
        }
        if (loanOffers != null) {
            log.debug("Loan offers from calculator service received successfully");
            loanOffers.stream().sorted((o1, o2) -> o2.getTotalAmount()
                            .compareTo(o1.getTotalAmount()))
                    .forEach(loanOfferDto -> loanOfferDto.setStatementId(statement.getStatementId()));
            return loanOffers;
        } else {
            log.error("Failed to get loan offers from calculator service");
            throw new CalculatorServiceException("Failed to get loan offers from calculator service");
        }
    }
}

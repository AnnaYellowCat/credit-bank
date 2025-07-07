package com.neoflex.statementservice.services;

import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.dto.LoanStatementRequestDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.UnderageException;
import com.neoflex.statementservice.validators.AgeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class FetchOffersService {
    private final AgeValidator ageValidator;
    private final RestTemplate restTemplate;

    public FetchOffersService(AgeValidator ageValidator, RestTemplate restTemplate) {
        this.ageValidator = ageValidator;
        this.restTemplate = restTemplate;
    }

    @Value("${app.deal.get}")
    private String urlGetOffers;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto statementRequest) {
        if (!ageValidator.isAdult(statementRequest.getBirthDate())) {
            log.error("Age of client is less than 18, date of birth: {}", statementRequest.getBirthDate());
            throw new UnderageException("Age must be at least 18");
        }

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
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            log.error("Failed to get loan offers from deal service, status code {}", e.getStatusCode());
            throw new DealServiceException("Failed to get loan offers from deal service");
        } catch (Exception e) {
            log.error("Failed to get loan offers from deal service due to unexpected error: {}", e.getMessage());
            throw new DealServiceException("Failed to get loan offers from deal service due to unexpected error");
        }

        if (loanOffers != null) {
            log.debug("Loan offers from deal service received successfully");
            loanOffers.sort((o1, o2) -> o2.getTotalAmount()
                    .compareTo(o1.getTotalAmount()));
            return loanOffers;
        } else {
            log.error("Failed to get loan offers from deal service, loan offers is null");
            throw new DealServiceException("Failed to get loan offers from deal service, loan offers is null");
        }
    }
}

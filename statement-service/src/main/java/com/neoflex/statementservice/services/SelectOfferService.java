package com.neoflex.statementservice.services;

import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.StatementNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SelectOfferService {
    private final RestTemplate restTemplate;

    public SelectOfferService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${app.deal.select}")
    private String urlSelectOffer;

    public void selectOffer(LoanOfferDto offer) {
        try {
            restTemplate.exchange(
                    urlSelectOffer,
                    HttpMethod.POST,
                    new HttpEntity<>(offer),
                    Void.class
            );
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.error("Statement with id {} not found in deal service", offer.getStatementId());
                throw new StatementNotFoundException("Statement not found");
            }
            log.error("Failed to send loan offer to deal service, status code {}", e.getStatusCode());
            throw new DealServiceException("Failed to send loan offer to deal service");
        } catch (Exception e) {
            log.error("Failed to send loan offer to deal service due to unexpected error: {}", e.getMessage());
            throw new DealServiceException("Failed to send loan offer to deal service due to unexpected error");
        }
    }
}

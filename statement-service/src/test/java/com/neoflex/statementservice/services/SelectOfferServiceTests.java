package com.neoflex.statementservice.services;

import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.StatementNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class SelectOfferServiceTests {
    @Autowired
    private SelectOfferService selectOfferService;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    public void selectOffer_NotThrowsExceptions_WhenDealServiceReturnsOK() {
        ResponseEntity<Void> response = ResponseEntity.status(HttpStatus.OK).build();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(response);

        selectOfferService.selectOffer(new LoanOfferDto());

        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        );
    }

    @Test
    public void selectOffer_ThrowsStatementNotFoundException_WhenDealServiceReturnsNotFound() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.NOT_FOUND);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenThrow(exception);

        assertThrows(StatementNotFoundException.class,
                () -> selectOfferService.selectOffer(new LoanOfferDto()));
    }

    @Test
    public void selectOffer_ThrowsDealServiceException_WhenDealServiceReturnsError() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenThrow(exception);

        assertThrows(DealServiceException.class,
                () -> selectOfferService.selectOffer(new LoanOfferDto()));
    }

    @Test
    public void selectOffer_ThrowsDealServiceException_WhenUnexpectedError() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenThrow(new RuntimeException());

        assertThrows(DealServiceException.class,
                () -> selectOfferService.selectOffer(new LoanOfferDto()));
    }
}

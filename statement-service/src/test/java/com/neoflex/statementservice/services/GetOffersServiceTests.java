package com.neoflex.statementservice.services;

import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.dto.LoanStatementRequestDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.UnderageException;
import com.neoflex.statementservice.validators.AgeValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class GetOffersServiceTests {
    @Autowired
    private GetOffersService getOffersService;

    @MockitoBean
    private AgeValidator ageValidator;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    public void getOffers_ReturnsOffers_WhenAgeIsAbove18AndDealServiceReturnsOffers() {
        when(ageValidator.isAdult(any())).thenReturn(true);
        List<LoanOfferDto> loanOffersResponse = new ArrayList<>();
        loanOffersResponse.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(40000)).build());
        loanOffersResponse.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(30000)).build());
        loanOffersResponse.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(20000)).build());
        loanOffersResponse.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(10000)).build());
        ResponseEntity<List<LoanOfferDto>> mockResponse = ResponseEntity.ok(loanOffersResponse);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        List<LoanOfferDto> offers = getOffersService.getOffers(new LoanStatementRequestDto());

        assertEquals(4, offers.size());
        for (int i = 0; i < offers.size() - 1; i++) {
            assertTrue(offers.get(i).getTotalAmount()
                    .compareTo(offers.get(i + 1).getTotalAmount()) >= 0);
        }
    }

    @Test
    public void getOffers_ThrowsUnderageException_WhenAgeIsUnder18() {
        when(ageValidator.isAdult(any())).thenReturn(false);

        assertThrows(UnderageException.class,
                () -> getOffersService.getOffers(new LoanStatementRequestDto()));
    }

    @Test
    public void getOffers_ThrowsDealServiceException_WhenDealServiceReturnsError() {
        when(ageValidator.isAdult(any())).thenReturn(true);
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception);

        assertThrows(DealServiceException.class,
                () -> getOffersService.getOffers(new LoanStatementRequestDto()));
    }

    @Test
    public void getOffers_ThrowsDealServiceException_WhenUnexpectedError() {
        when(ageValidator.isAdult(any())).thenReturn(true);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException());

        assertThrows(DealServiceException.class,
                () -> getOffersService.getOffers(new LoanStatementRequestDto()));
    }

    @Test
    public void getOffers_ThrowsDealServiceException_WhenDealServiceReturnsNull() {
        when(ageValidator.isAdult(any())).thenReturn(true);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(DealServiceException.class,
                () -> getOffersService.getOffers(new LoanStatementRequestDto()));
    }
}

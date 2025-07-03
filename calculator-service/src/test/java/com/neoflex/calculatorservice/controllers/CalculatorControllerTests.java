package com.neoflex.calculatorservice.controllers;

import com.neoflex.calculatorservice.dto.*;
import com.neoflex.calculatorservice.exceptions.LoanDeniedException;
import com.neoflex.calculatorservice.services.CalculatorService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.restassured.http.ContentType;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CalculatorControllerTests {
    @InjectMocks
    private CalculatorController calculatorController;

    @MockitoBean
    private CalculatorService calculatorService;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void beforeEach() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void getOffers_ReturnsLoanOffers(){
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(36)
                .firstName("Ivan")
                .lastName("Ivanov")
                .middleName("Ivanovich")
                .email("ivan@mail.ru")
                .birthDate(LocalDate.parse("2000-01-01"))
                .passportSeries("4567")
                .passportNumber("456789")
                .build();
        List<LoanOfferDto> offers = List.of(
                LoanOfferDto.builder()
                        .totalAmount(BigDecimal.valueOf(40000))
                        .rate(BigDecimal.valueOf(20))
                        .build(),
                LoanOfferDto.builder()
                        .totalAmount(BigDecimal.valueOf(30000))
                        .rate(BigDecimal.valueOf(16))
                        .build(),
                LoanOfferDto.builder()
                        .totalAmount(BigDecimal.valueOf(20000))
                        .rate(BigDecimal.valueOf(14))
                        .build(),
                LoanOfferDto.builder()
                        .totalAmount(BigDecimal.valueOf(10000))
                        .rate(BigDecimal.valueOf(10))
                        .build()
        );
        when(calculatorService.getLoanOffers(any(LoanStatementRequestDto.class)))
                .thenReturn(offers);

        List result = given()
                .contentType(ContentType.JSON)
                .when()
                .body(loanStatementRequestDto)
                .post("/calculator/offers")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value()).extract().as(List.class);

        assertEquals(4, result.size());
    }

    @Test
    void getCredit_ReturnsCredit_WhenItIsAvailableToIssueLoan(){
        when(calculatorService.getCredit(any(ScoringDataDto.class)))
                .thenReturn(CreditDto.builder()
                        .psk(BigDecimal.valueOf(100000))
                        .term(10)
                        .rate(BigDecimal.valueOf(10))
                        .monthlyPayment(BigDecimal.valueOf(100))
                        .paymentSchedule(new ArrayList<>(10))
                        .build());

        CreditDto creditDto = given()
                .contentType(ContentType.JSON)
                .when()
                .body(ScoringDataDto.builder().amount(BigDecimal.valueOf(100000)).build())
                .post("/calculator/calc")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value()).extract().as(CreditDto.class);

        assertEquals(BigDecimal.valueOf(100000), creditDto.getPsk().setScale(0, RoundingMode.DOWN));
    }

    @Test
    void getCredit_ReturnsInternalServerError_WhenItIsNotAvailableToIssueLoan(){
        when(calculatorService.getCredit(any(ScoringDataDto.class)))
                .thenThrow(new LoanDeniedException("Age more than 70 years"));

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(new ScoringDataDto())
                .post("/calculator/calc")
                .then()
                .log().body()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}

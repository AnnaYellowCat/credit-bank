package com.neoflex.statementservice.controllers;

import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.dto.LoanStatementRequestDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.StatementNotFoundException;
import com.neoflex.statementservice.services.GetOffersService;
import com.neoflex.statementservice.services.SelectOfferService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StatementControllerTests {
    @MockitoBean
    private GetOffersService getOffersService;

    @MockitoBean
    private SelectOfferService selectOfferService;

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void beforeEach() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void getOffers_ReturnsLoanOffers_WhenInputDataIsValid() {
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(36)
                .firstName("Rose")
                .lastName("Vanderboom")
                .middleName("Albertovna")
                .email("rose@gmail.com")
                .birthDate(LocalDate.parse("1909-03-01"))
                .passportSeries("1234")
                .passportNumber("123456")
                .build();
        List<LoanOfferDto> offers = new ArrayList<>();
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(40000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(30000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(20000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(10000)).build());
        when(getOffersService.getOffers(any(LoanStatementRequestDto.class)))
                .thenReturn(offers);

        List result = given()
                .contentType(ContentType.JSON)
                .when()
                .body(loanStatementRequestDto)
                .post("/statement")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value()).extract().as(List.class);

        assertEquals(4, result.size());
    }

    @Test
    void getOffers_ReturnsBadRequest_WhenInputDataIsInvalid() {
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(10))
                .term(1)
                .firstName("567890")
                .lastName("89709")
                .middleName("899")
                .email("00000000")
                .birthDate(LocalDate.parse("1909-03-01"))
                .passportSeries("aaaaaaa")
                .passportNumber("aa")
                .build();
        List<LoanOfferDto> offers = new ArrayList<>();
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(40000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(30000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(20000)).build());
        offers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(10000)).build());
        when(getOffersService.getOffers(any(LoanStatementRequestDto.class)))
                .thenReturn(offers);

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(loanStatementRequestDto)
                .post("/statement")
                .then()
                .log().body()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void getOffers_ReturnsFailedDependency_WhenErrorDuringRequestToDealService() {
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(36)
                .firstName("Rose")
                .lastName("Vanderboom")
                .middleName("Albertovna")
                .email("rose@gmail.com")
                .birthDate(LocalDate.parse("1909-03-01"))
                .passportSeries("1234")
                .passportNumber("123456")
                .build();
        when(getOffersService.getOffers(any(LoanStatementRequestDto.class)))
                .thenThrow(new DealServiceException(""));

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(loanStatementRequestDto)
                .post("/statement")
                .then()
                .log().body()
                .statusCode(HttpStatus.FAILED_DEPENDENCY.value());
    }

    @Test
    void selectOffer_ReturnsOk_WhenNoExceptions() {
        LoanOfferDto offer = LoanOfferDto.builder()
                .statementId(UUID.randomUUID())
                .requestedAmount(new BigDecimal("100000"))
                .totalAmount(new BigDecimal("120000"))
                .term(36)
                .monthlyPayment(new BigDecimal("10000"))
                .rate(BigDecimal.valueOf(15))
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .build();
        doNothing().when(selectOfferService)
                .selectOffer(any(LoanOfferDto.class));

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(offer)
                .post("/statement/offer")
                .then()
                .log().body()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    void selectOffer_ReturnsFailedDependency_WhenErrorDuringRequestToDealService() {
        LoanOfferDto offer = LoanOfferDto.builder()
                .statementId(UUID.randomUUID())
                .requestedAmount(new BigDecimal("100000"))
                .totalAmount(new BigDecimal("120000"))
                .term(36)
                .monthlyPayment(new BigDecimal("10000"))
                .rate(BigDecimal.valueOf(15))
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .build();
        doThrow(new DealServiceException("")).when(selectOfferService)
                .selectOffer(any(LoanOfferDto.class));

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(offer)
                .post("/statement/offer")
                .then()
                .log().body()
                .statusCode(HttpStatus.FAILED_DEPENDENCY.value());
    }

    @Test
    void selectOffer_ReturnsNotFound_WhenErrorDuringRequestToDealService() {
        LoanOfferDto offer = LoanOfferDto.builder()
                .statementId(UUID.randomUUID())
                .requestedAmount(new BigDecimal("100000"))
                .totalAmount(new BigDecimal("120000"))
                .term(36)
                .monthlyPayment(new BigDecimal("10000"))
                .rate(BigDecimal.valueOf(15))
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .build();
        doThrow(new StatementNotFoundException("")).when(selectOfferService)
                .selectOffer(any(LoanOfferDto.class));

        given()
                .contentType(ContentType.JSON)
                .when()
                .body(offer)
                .post("/statement/offer")
                .then()
                .log().body()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}

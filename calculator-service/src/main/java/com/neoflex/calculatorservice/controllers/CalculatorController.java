package com.neoflex.calculatorservice.controllers;

import com.neoflex.calculatorservice.dto.*;
import com.neoflex.calculatorservice.exceptions.LoanDeniedException;
import com.neoflex.calculatorservice.services.CalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.util.List;

@Slf4j
@RestController
@Tag(name = "Calculator controller",
        description = "Controller for calculation of loan parameters"
)
public class CalculatorController {
    private static final int ROUNDING_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @Operation(
            summary = "Gets loan offers",
            description = "Gets loan offers with all possible combinations of parameters " +
                    "by info from LoanStatementRequestDto"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan offers successfully generated")
    })
    @PostMapping("/calculator/offers")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Loan offers request for {} {}, amount: {}, term: {} months",
                loanStatementRequestDto.getFirstName(),
                loanStatementRequestDto.getLastName(),
                loanStatementRequestDto.getAmount().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                loanStatementRequestDto.getTerm());
        List<LoanOfferDto> offers = calculatorService.getLoanOffers(loanStatementRequestDto);
        log.info("Generated {} loan offers", offers.size());
        for (int i=0; i<offers.size(); i++) {
            log.info("Offer {} - rate: {}%, total amount: {}",
                    i+1, offers.get(i).getRate().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    offers.get(i).getTotalAmount().setScale(ROUNDING_SCALE, ROUNDING_MODE));
        }
        return ResponseEntity.ok(offers);
    }

    @Operation(
            summary = "Gets credit",
            description = "Gets parameters for credit by info from ScoringDataDto"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit calculated successfully"),
            @ApiResponse(responseCode = "500", description = "Loan denied")
    })
    @PostMapping("/calculator/calc")
    public ResponseEntity<?> getCredit(@RequestBody ScoringDataDto scoringDataDto) {
        try {
            log.info("Received credit calculation request for {} {}, amount: {}, term: {} months",
                    scoringDataDto.getFirstName(),
                    scoringDataDto.getLastName(),
                    scoringDataDto.getAmount().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    scoringDataDto.getTerm());
            CreditDto credit = calculatorService.getCredit(scoringDataDto);
            log.info("Credit calculation result - total amount: {}, term: {}, rate: {}, monthly payment: {}, payment schedule with {} elements",
                    credit.getPsk().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    credit.getTerm(),
                    credit.getRate().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    credit.getMonthlyPayment().setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    credit.getPaymentSchedule().size());
            return ResponseEntity.ok(credit);
        }
        catch (LoanDeniedException e) {
            return ResponseEntity
                    .status(500)
                    .body(ErrorDto.builder()
                            .denialReason(e.getMessage())
                            .build());
        }
    }
}

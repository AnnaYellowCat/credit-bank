package com.neoflex.calculatorservice.controllers;

import com.neoflex.calculatorservice.dto.CreditDto;
import com.neoflex.calculatorservice.dto.LoanOfferDto;
import com.neoflex.calculatorservice.dto.LoanStatementRequestDto;
import com.neoflex.calculatorservice.dto.ScoringDataDto;
import com.neoflex.calculatorservice.services.CalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Calculator controller",
        description = "Controller for calculation of loan parameters"
)
public class CalculatorController {
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
            @ApiResponse(responseCode = "200", description = "Loan offers successfully generated"),
            @ApiResponse(responseCode = "400", description = "Invalid data")
    })
    @PostMapping("/calculator/offers")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@Valid @RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Received request for loan offers with data: {}", loanStatementRequestDto);
        List<LoanOfferDto> offers = calculatorService.getLoanOffers(loanStatementRequestDto);
        log.info("Generated loan offers: {}", offers);
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
    public ResponseEntity<CreditDto> getCredit(@RequestBody ScoringDataDto scoringDataDto) {
        log.info("Received request for credit calculation with data: {}", scoringDataDto);
        CreditDto credit = calculatorService.getCredit(scoringDataDto);
        log.info("Calculated credit info: {}", credit);
        return ResponseEntity.ok(credit);
    }
}

package com.neoflex.calculatorservice.controllers;

import com.neoflex.calculatorservice.dto.CreditDto;
import com.neoflex.calculatorservice.dto.LoanOfferDto;
import com.neoflex.calculatorservice.dto.LoanStatementRequestDto;
import com.neoflex.calculatorservice.dto.ScoringDataDto;
import com.neoflex.calculatorservice.services.CalculatorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class CalculatorController {
    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @PostMapping("/calculator/offers")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@Valid @RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Loan offers request for {} {}, amount: {}, term: {} months",
                loanStatementRequestDto.getFirstName(),
                loanStatementRequestDto.getLastName(),
                loanStatementRequestDto.getAmount(),
                loanStatementRequestDto.getTerm());
        List<LoanOfferDto> offers = calculatorService.getLoanOffers(loanStatementRequestDto);
        log.info("Generated {} loan offers", offers.size());
        for (int i=0; i<offers.size(); i++) {
            log.info("Offer {} - rate: {}%, total amount: {}",
                    i+1, offers.get(i).getRate(), offers.get(i).getTotalAmount());
        }
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/calculator/calc")
    public ResponseEntity<CreditDto> getCredit(@RequestBody ScoringDataDto scoringDataDto) {
        log.info("Received credit calculation request for {} {}, amount: {}, term: {} months",
                scoringDataDto.getFirstName(),
                scoringDataDto.getLastName(),
                scoringDataDto.getAmount(),
                scoringDataDto.getTerm());
        CreditDto credit = calculatorService.getCredit(scoringDataDto);
        log.info("Credit calculation result - total amount: {}, term: {}, rate: {}, monthly payment: {}, payment schedule with {} elements",
                credit.getPsk(),
                credit.getTerm(),
                credit.getRate(),
                credit.getMonthlyPayment(),
                credit.getPaymentSchedule().size());
        return ResponseEntity.ok(credit);
    }
}

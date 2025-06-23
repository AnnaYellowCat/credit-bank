package com.neoflex.dealservice.controllers;

import com.neoflex.dealservice.api.DealApi;
import com.neoflex.dealservice.dto.FinishRegistrationRequestDto;
import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.services.FinishRegistrationService;
import com.neoflex.dealservice.services.CreateStatementService;
import com.neoflex.dealservice.services.SelectOfferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/deal")
public class DealController implements DealApi {
    private final CreateStatementService createStatementService;
    private final SelectOfferService selectOfferService;
    private final FinishRegistrationService finishRegistrationService;

    public DealController(CreateStatementService createStatementService, FinishRegistrationService finishRegistrationService, SelectOfferService selectOfferService) {
        this.createStatementService = createStatementService;
        this.finishRegistrationService = finishRegistrationService;
        this.selectOfferService = selectOfferService;
    }

    @Override
    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Received request for loan offers: loan amount {}, term {}",
                loanStatementRequestDto.getAmount(), loanStatementRequestDto.getTerm());
        try{
            List<LoanOfferDto> offers = createStatementService.getOffers(loanStatementRequestDto);
            return ResponseEntity.ok(offers);
        }
        catch(CalculatorServiceException e){
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/offer/select")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Received request for choosing loan offer: rate {}, total amount {}",
                loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
        try{
            selectOfferService.selectOffer(loanOfferDto);
            return ResponseEntity.ok().build();
        }
        catch(StatementNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Override
    @PostMapping("/calculate/{statementId}")
    public ResponseEntity<Void> finishRegistration(@PathVariable String statementId,
                                                   @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Received request for completion of registration for statement with id {}", statementId);
        try{
            finishRegistrationService.finishRegistration(finishRegistrationRequestDto, statementId);
            return ResponseEntity.ok().build();
        }
        catch(StatementNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        catch(CalculatorServiceException e){
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }
}

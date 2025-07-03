package com.neoflex.statementservice.controllers;

import com.neoflex.statementservice.api.StatementApi;
import com.neoflex.statementservice.dto.LoanOfferDto;
import com.neoflex.statementservice.dto.LoanStatementRequestDto;
import com.neoflex.statementservice.exceptions.DealServiceException;
import com.neoflex.statementservice.exceptions.StatementNotFoundException;
import com.neoflex.statementservice.exceptions.UnderageException;
import com.neoflex.statementservice.services.GetOffersService;
import com.neoflex.statementservice.services.SelectOfferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/statement")
public class StatementController implements StatementApi {
    private final GetOffersService getOffersService;
    private final SelectOfferService selectOfferService;

    public StatementController(GetOffersService getOffersService, SelectOfferService selectOfferService) {
        this.getOffersService = getOffersService;
        this.selectOfferService = selectOfferService;
    }

    @Override
    @PostMapping()
    public ResponseEntity<List<LoanOfferDto>> getOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Received request for loan offers: loan amount {}, term {}",
                loanStatementRequestDto.getAmount(), loanStatementRequestDto.getTerm());
        try {
            List<LoanOfferDto> offers = getOffersService.getOffers(loanStatementRequestDto);
            log.info("Success: loan offers with total amount {}, {}, {} and {} returned", offers.get(0).getTotalAmount(),
                    offers.get(1).getTotalAmount(), offers.get(2).getTotalAmount(), offers.get(3).getTotalAmount());
            return ResponseEntity.ok(offers);
        } catch (UnderageException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (DealServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/offer")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Received request for choosing loan offer: rate {}, total amount {}",
                loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
        try {
            selectOfferService.selectOffer(loanOfferDto);
            log.info("Success: selected loan offer with rate {} and total amount {} sent",
                    loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
            return ResponseEntity.ok().build();
        } catch (DealServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

package com.neoflex.dealservice.controllers;

import com.neoflex.dealservice.api.DealApi;
import com.neoflex.dealservice.dto.CreditDto;
import com.neoflex.dealservice.dto.FinishRegistrationRequestDto;
import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.exceptions.*;
import com.neoflex.dealservice.services.*;
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
    private final SendDocumentsService sendDocumentsService;
    private final SendCodeService sendCodeService;
    private final IssueCreditService issueCreditService;
    private final FetchCreditInfoService fetchCreditInfoService;

    public DealController(CreateStatementService createStatementService, FinishRegistrationService finishRegistrationService,
                          SelectOfferService selectOfferService, SendDocumentsService sendDocumentsService,
                          SendCodeService sendCodeService, IssueCreditService issueCreditService, FetchCreditInfoService fetchCreditInfoService) {
        this.createStatementService = createStatementService;
        this.finishRegistrationService = finishRegistrationService;
        this.selectOfferService = selectOfferService;
        this.sendDocumentsService = sendDocumentsService;
        this.sendCodeService = sendCodeService;
        this.issueCreditService = issueCreditService;
        this.fetchCreditInfoService = fetchCreditInfoService;
    }

    @Override
    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Received request for loan offers: loan amount {}, term {}",
                loanStatementRequestDto.getAmount(), loanStatementRequestDto.getTerm());
        try {
            List<LoanOfferDto> offers = createStatementService.getOffers(loanStatementRequestDto);
            log.info("Success: loan offers with total amount {}, {}, {} and {} returned", offers.get(0).getTotalAmount(),
                    offers.get(1).getTotalAmount(), offers.get(2).getTotalAmount(), offers.get(3).getTotalAmount());
            return ResponseEntity.ok(offers);
        } catch (CalculatorServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/offer/select")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Received request for choosing loan offer: rate {}, total amount {}",
                loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
        try {
            selectOfferService.selectOffer(loanOfferDto);
            log.info("Success: selected loan offer with rate {} and total amount {} saved",
                    loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
            return ResponseEntity.ok().build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentIsAlreadySignedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    @PostMapping("/calculate/{statementId}")
    public ResponseEntity<Void> finishRegistration(@PathVariable String statementId,
                                                   @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Received request for completion of registration for statement with id {}", statementId);
        try {
            finishRegistrationService.finishRegistration(finishRegistrationRequestDto, statementId);
            log.info("Success: registration finished for statement with id {}", statementId);
            return ResponseEntity.ok().build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentIsAlreadySignedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (CalculatorServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }


    @Override
    @PostMapping("/document/{statementId}/send")
    public ResponseEntity<Void> sendDocumentsRequest(@PathVariable String statementId) {
        log.info("Received request for creation documents for statement with id {}", statementId);
        try {
            sendDocumentsService.sendDocsCreationRequest(statementId);
            log.info("Success: request for creation documents for statement with id {} sent", statementId);
            return ResponseEntity.ok().build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentIsAlreadySignedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    @PostMapping("/document/{statementId}/sign")
    public ResponseEntity<Void> signDocumentsRequest(@PathVariable String statementId) {
        log.info("Received request for creation code for statement with id {}", statementId);
        try {
            sendCodeService.sendCodeCreationRequest(statementId);
            log.info("Success: request for creation code for statement with id {} sent", statementId);
            return ResponseEntity.ok().build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentIsAlreadySignedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @Override
    @PostMapping("/document/{statementId}/code")
    public ResponseEntity<Void> signDocuments(@PathVariable String statementId, @RequestParam String code) {
        log.info("Received request for signing documents for statement with id {}", statementId);
        try {
            issueCreditService.issueCredit(statementId, code);
            log.info("Success: documents for statement with id {} signed", statementId);
            return ResponseEntity.ok().build();
        } catch (StatementNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DocumentIsAlreadySignedException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (InvalidCodeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Override
    @GetMapping("/credit/{statementId}")
    public ResponseEntity<CreditDto> getCredit(@PathVariable String statementId) {
        log.info("Received request for getting credit for statement with id {}", statementId);
        try {
            CreditDto creditDto = fetchCreditInfoService.getCreditInfo(statementId);
            log.info("Success: credit for statement with id {} got", statementId);
            return ResponseEntity.ok(creditDto);
        } catch (StatementNotFoundException | CreditNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

package com.neoflex.gatewayservice.controllers;

import com.neoflex.gatewayservice.api.GatewayApi;
import com.neoflex.gatewayservice.dto.FinishRegistrationRequestDto;
import com.neoflex.gatewayservice.dto.LoanOfferDto;
import com.neoflex.gatewayservice.dto.LoanStatementRequestDto;
import com.neoflex.gatewayservice.dto.StatementDto;
import com.neoflex.gatewayservice.exceptions.ClientHttpException;
import com.neoflex.gatewayservice.exceptions.ExternalServiceException;
import com.neoflex.gatewayservice.services.GatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class GatewayController implements GatewayApi {
    private final GatewayService gatewayService;

    @Override
    @PostMapping("/statement")
    public ResponseEntity<List<LoanOfferDto>> getOffers(@RequestBody LoanStatementRequestDto loanStatementRequestDto) {
        log.info("Received request for loan offers: loan amount {}, term {}",
                loanStatementRequestDto.getAmount(), loanStatementRequestDto.getTerm());
        try {
            List<LoanOfferDto> offers = gatewayService.getOffers(loanStatementRequestDto);
            log.info("Success: loan offers with total amount {}, {}, {} and {} returned", offers.get(0).getTotalAmount(),
                    offers.get(1).getTotalAmount(), offers.get(2).getTotalAmount(), offers.get(3).getTotalAmount());
            return ResponseEntity.ok(offers);
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/offer")
    public ResponseEntity<Void> selectOffer(@RequestBody LoanOfferDto loanOfferDto) {
        log.info("Received request for choosing loan offer: rate {}, total amount {}",
                loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
        try {
            gatewayService.selectOffer(loanOfferDto);
            log.info("Success: selected loan offer with rate {} and total amount {} sent",
                    loanOfferDto.getRate(), loanOfferDto.getTotalAmount());
            return ResponseEntity.ok().build();
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/calculate/{statementId}")
    public ResponseEntity<Void> finishRegistration(@PathVariable String statementId,
                                                   @RequestBody FinishRegistrationRequestDto finishRegistrationRequestDto) {
        log.info("Received request for completion of registration for statement with id {}", statementId);
        try {
            gatewayService.finishReg(statementId, finishRegistrationRequestDto);
            log.info("Success: registration finished for statement with id {}", statementId);
            return ResponseEntity.ok().build();
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/document/{statementId}/send")
    public ResponseEntity<Void> sendDocumentsRequest(@PathVariable String statementId) {
        log.info("Received request for creation documents for statement with id {}", statementId);
        try {
            gatewayService.sendDocsCreationRequest(statementId);
            log.info("Success: request for creation documents for statement with id {} sent", statementId);
            return ResponseEntity.ok().build();
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/document/{statementId}/sign")
    public ResponseEntity<Void> signDocumentsRequest(@PathVariable String statementId) {
        log.info("Received request for creation code for statement with id {}", statementId);
        try {
            gatewayService.sendCodeCreationRequest(statementId);
            log.info("Success: request for creation code for statement with id {} sent", statementId);
            return ResponseEntity.ok().build();
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @PostMapping("/document/{statementId}/code")
    public ResponseEntity<Void> signDocuments(@PathVariable String statementId, @RequestParam String code) {
        log.info("Received request for signing documents for statement with id {}", statementId);
        try {
            gatewayService.signDocuments(statementId, code);
            log.info("Success: documents for statement with id {} signed", statementId);
            return ResponseEntity.ok().build();
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @GetMapping("/admin/statement/{statementId}")
    public ResponseEntity<StatementDto> getStatement(@PathVariable String statementId) {
        log.info("Received request for getting statement with id {}", statementId);
        try {
            StatementDto statementDto = gatewayService.getStatement(statementId);
            log.info("Success: statement with id {} got", statementId);
            return ResponseEntity.ok(statementDto);
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }

    @Override
    @GetMapping("/admin/statement")
    public ResponseEntity<List<StatementDto>> getStatements() {
        log.info("Received request for getting all statements");
        try {
            List<StatementDto> statements = gatewayService.getStatements();
            log.info("Success: {} statement got", statements.size());
            return ResponseEntity.ok(statements);
        } catch (ClientHttpException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (ExternalServiceException e) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).build();
        }
    }
}

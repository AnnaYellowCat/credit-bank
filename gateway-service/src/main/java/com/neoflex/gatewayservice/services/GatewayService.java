package com.neoflex.gatewayservice.services;

import com.neoflex.gatewayservice.dto.FinishRegistrationRequestDto;
import com.neoflex.gatewayservice.dto.LoanOfferDto;
import com.neoflex.gatewayservice.dto.LoanStatementRequestDto;
import com.neoflex.gatewayservice.dto.StatementDto;
import com.neoflex.gatewayservice.exceptions.ClientHttpException;
import com.neoflex.gatewayservice.exceptions.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;
import org.springframework.web.client.RestClientException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
    private final RestClient restClient;

    @Value("${app.statement.get-offers}")
    private String urlGetOffers;
    @Value("${app.statement.select-offer}")
    private String urlSelectOffer;
    @Value("${app.deal.finish-reg}")
    private String urlFinishReg;
    @Value("${app.deal.send-documents-request.part1}")
    private String urlSendDocumentsPart1;
    @Value("${app.deal.send-documents-request.part2}")
    private String urlSendDocumentsPart2;
    @Value("${app.deal.sign-documents-request.part1}")
    private String urlSignDocumentsPart1;
    @Value("${app.deal.sign-documents-request.part2}")
    private String urlSignDocumentsPart2;
    @Value("${app.deal.sign-documents.part1}")
    private String urlCodePart1;
    @Value("${app.deal.sign-documents.part2}")
    private String urlCodePart2;
    @Value("${app.deal.get-statement}")
    private String urlGetStatement;
    @Value("${app.deal.get-statements}")
    private String urlGetStatements;

    public List<LoanOfferDto> getOffers(LoanStatementRequestDto statementRequest) {
        return sendPostRequestWithResponse(urlGetOffers, statementRequest, new ParameterizedTypeReference<>() {
                },
                "get loan offers from statement service");
    }

    public void selectOffer(LoanOfferDto loanOfferDto) {
        sendPostRequestWithoutResponse(urlSelectOffer, loanOfferDto,
                "select loan offer from statement service");
    }

    public void finishReg(String statementId, FinishRegistrationRequestDto finishRegRequest) {
        sendPostRequestWithoutResponse(urlFinishReg + statementId, finishRegRequest,
                "finish reg in deal service");
    }

    public void sendDocsCreationRequest(String statementId) {
        sendPostRequestWithoutResponse(urlSendDocumentsPart1 + statementId + urlSendDocumentsPart2, null,
                "send request for creation documents in deal service");
    }

    public void sendCodeCreationRequest(String statementId) {
        sendPostRequestWithoutResponse(urlSignDocumentsPart1 + statementId + urlSignDocumentsPart2, null,
                "send request for signing documents in deal service");
    }

    public void signDocuments(String statementId, String code) {
        sendPostRequestWithoutResponse(urlCodePart1 + statementId + urlCodePart2 + code, null,
                "sign documents in deal service");
    }

    public StatementDto getStatement(String statementId) {
        return sendGetRequest(urlGetStatement + statementId, new ParameterizedTypeReference<>() {
                },
                "get statement from deal service");
    }

    public List<StatementDto> getStatements() {
        return sendGetRequest(urlGetStatements, new ParameterizedTypeReference<>() {
                },
                "get statements from deal service");
    }

    private <T> T sendPostRequestWithResponse(String url, Object body, ParameterizedTypeReference<T> responseType, String errorText) {
        try {
            return restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, getServerErrorHandler(errorText))
                    .onStatus(HttpStatusCode::is4xxClientError, getClientErrorHandler(errorText))
                    .body(responseType);
        } catch (RestClientException e) {
            handleUnexpectedException(e, errorText);
            throw new ExternalServiceException("Failed to " + errorText + " due to unexpected error");
        }
    }

    private void sendPostRequestWithoutResponse(String url, Object body, String errorText) {
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(url);
            if (body != null) {
                request.body(body);
            }
            request.retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, getServerErrorHandler(errorText))
                    .onStatus(HttpStatusCode::is4xxClientError, getClientErrorHandler(errorText))
                    .toBodilessEntity();
        } catch (RestClientException e) {
            handleUnexpectedException(e, errorText);
            throw new ExternalServiceException("Failed to " + errorText + " due to unexpected error");
        }
    }

    private <T> T sendGetRequest(String url, ParameterizedTypeReference<T> responseType, String errorText) {
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, getServerErrorHandler(errorText))
                    .onStatus(HttpStatusCode::is4xxClientError, getClientErrorHandler(errorText))
                    .body(responseType);
        } catch (RestClientException e) {
            handleUnexpectedException(e, errorText);
            throw new ExternalServiceException("Failed to " + errorText + " due to unexpected error");
        }
    }

    private ErrorHandler getServerErrorHandler(String errorText) {
        return (request, response) -> {
            log.error("Failed to {} due to server error, status code: {}", errorText, response.getStatusCode());
            throw new ExternalServiceException("Deal or statement service internal error");
        };
    }

    private ErrorHandler getClientErrorHandler(String errorText) {
        return (request, response) -> {
            log.error("Failed to {} due to client error, status code: {}", errorText, response.getStatusCode());
            throw new ClientHttpException(response.getStatusCode());
        };
    }

    private void handleUnexpectedException(RestClientException e, String errorText) {
        log.error("Failed to {} due to unexpected error: {}", errorText, e.getMessage());
    }
}
package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.dto.LoanStatementRequestDto;
import com.neoflex.dealservice.dto.StatementStatusHistoryDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.repositories.ClientRepository;
import com.neoflex.dealservice.repositories.StatementRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.neoflex.dealservice.enums.ApplicationStatus.PREAPPROVAL;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class CreateStatementServiceTests {
    @Autowired
    private CreateStatementService createStatementService;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private StatementRepository statementRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    public void getOffers_SavesDataAndReturnsLoanOffers_WhenCalculatorServiceReturnsOffers() {
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
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<LoanOfferDto> loanOffers = new ArrayList<>();
        loanOffers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(40000)).build());
        loanOffers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(30000)).build());
        loanOffers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(20000)).build());
        loanOffers.add(LoanOfferDto.builder().totalAmount(BigDecimal.valueOf(10000)).build());
        ResponseEntity<List<LoanOfferDto>> mockResponse = ResponseEntity.ok(loanOffers);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);

        List<LoanOfferDto> offers = createStatementService.getOffers(loanStatementRequestDto);

        verify(clientRepository).save(clientCaptor.capture());
        Client client = clientCaptor.getValue();
        assertNotNull(client.getClientId());
        assertEquals("Rose", client.getFirstName());
        assertEquals("Vanderboom", client.getLastName());
        assertEquals("Albertovna", client.getMiddleName());
        assertEquals("rose@gmail.com", client.getEmail());
        assertEquals(LocalDate.parse("1909-03-01"), client.getBirthDate());
        assertNotNull(client.getPassport().getPassportId());
        assertEquals("1234", client.getPassport().getSeries());
        assertEquals("123456", client.getPassport().getNumber());
        verify(statementRepository).save(statementCaptor.capture());
        Statement statement = statementCaptor.getValue();
        assertNotNull(statement.getStatementId());
        assertEquals(client.getClientId(), statement.getClient().getClientId());
        assertEquals(PREAPPROVAL, statement.getStatus());
        assertNotNull(statement.getCreationDate());
        StatementStatusHistoryDto statusHistoryElement = statement.getStatusHistory().getLast();
        assertEquals(PREAPPROVAL, statusHistoryElement.getStatus());
        assertEquals(statement.getCreationDate(), statusHistoryElement.getTime());
        assertEquals(AUTOMATIC, statusHistoryElement.getChangeType());
        for (int i = 0; i < offers.size() - 1; i++) {
            assertTrue(offers.get(i).getTotalAmount()
                    .compareTo(offers.get(i + 1).getTotalAmount()) >= 0);
        }
        assertEquals(statement.getStatementId(), offers.get(0).getStatementId());
        assertEquals(statement.getStatementId(), offers.get(1).getStatementId());
        assertEquals(statement.getStatementId(), offers.get(2).getStatementId());
        assertEquals(statement.getStatementId(), offers.get(3).getStatementId());
    }

    @Test
    public void getOffers_ThrowsCalculatorServiceException_WhenCalculatorServiceReturnsStatus503() {
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
        when(clientRepository.save(any(Client.class))).thenReturn(null);
        when(statementRepository.save(any(Statement.class))).thenReturn(null);
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception);

        assertThrows(CalculatorServiceException.class,
                () -> createStatementService.getOffers(loanStatementRequestDto));
    }
}

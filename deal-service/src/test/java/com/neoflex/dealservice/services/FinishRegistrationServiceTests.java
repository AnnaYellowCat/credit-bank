package com.neoflex.dealservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neoflex.dealservice.dto.*;
import com.neoflex.dealservice.entities.*;
import com.neoflex.dealservice.exceptions.CalculatorServiceException;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.mappers.ClientMapper;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.ClientRepository;
import com.neoflex.dealservice.repositories.CreditRepository;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.*;
import static com.neoflex.dealservice.enums.CreditStatus.CALCULATED;
import static com.neoflex.dealservice.enums.EmploymentPosition.WORKER;
import static com.neoflex.dealservice.enums.EmploymentStatus.EMPLOYED;
import static com.neoflex.dealservice.enums.Gender.FEMALE;
import static com.neoflex.dealservice.enums.MaritalStatus.SINGLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class FinishRegistrationServiceTests {
    @Autowired
    private FinishRegistrationService finishRegistrationService;

    @MockitoBean
    private StatementRepository statementRepository;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private CreditRepository creditRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @Autowired
    private ClientMapper clientMapper;

    @Test
    public void finishRegistration_UpdatesDataAndCreatesCredit_WhenStatementExistsAndCalculatorServiceReturnsCredit() {
        UUID statementId = UUID.randomUUID();
        EmploymentDto employment = EmploymentDto.builder()
                .employmentStatus(String.valueOf(EMPLOYED))
                .employerINN("8476975937")
                .salary(BigDecimal.valueOf(30000))
                .position(String.valueOf(WORKER))
                .workExperienceTotal(60)
                .workExperienceCurrent(60)
                .build();
        FinishRegistrationRequestDto finishRegistrationRequestDto = FinishRegistrationRequestDto.builder()
                .gender(String.valueOf(FEMALE))
                .maritalStatus(String.valueOf(SINGLE))
                .dependentAmount(0)
                .passportIssueDate(LocalDate.parse("1930-03-01"))
                .passportIssueBranch("Rusty Lake")
                .employment(employment)
                .accountNumber("098768394578")
                .build();
        Client client = Client.builder()
                .passport(new Passport())
                .employment(clientMapper.createEmployment(employment))
                .build();
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        Statement statement = Statement.builder()
                .client(client)
                .status(APPROVED)
                .appliedOffer(new LoanOfferDto())
                .statusHistory(statusHistory)
                .build();
        List<PaymentScheduleElementDto> paymentSchedule = new ArrayList<>();
        paymentSchedule.add(new PaymentScheduleElementDto());
        paymentSchedule.add(new PaymentScheduleElementDto());
        paymentSchedule.add(new PaymentScheduleElementDto());
        CreditDto creditDto = CreditDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(36)
                .monthlyPayment(BigDecimal.valueOf(10000))
                .rate(BigDecimal.valueOf(15))
                .psk(BigDecimal.valueOf(120000))
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .paymentSchedule(paymentSchedule)
                .build();
        when(statementRepository.getReferenceById(statementId)).thenReturn(statement);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ResponseEntity<CreditDto> mockResponse = ResponseEntity.ok(creditDto);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);
        when(creditRepository.save(any(Credit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(kafkaProducer).sendMessage(any(EmailMessage.class), anyString());

        finishRegistrationService.finishRegistration(finishRegistrationRequestDto, String.valueOf(statementId));

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        Client updatedClient = clientCaptor.getValue();
        assertEquals(FEMALE, updatedClient.getGender());
        assertEquals(SINGLE, updatedClient.getMaritalStatus());
        assertEquals(0, updatedClient.getDependentAmount());
        assertEquals("098768394578", updatedClient.getAccountNumber());
        Passport clientPassport = updatedClient.getPassport();
        assertEquals(LocalDate.parse("1930-03-01"), clientPassport.getIssueDate());
        assertEquals("Rusty Lake", clientPassport.getIssueBranch());
        Employment clientEmployment = client.getEmployment();
        assertNotNull(clientEmployment.getEmploymentId());
        assertEquals(EMPLOYED, clientEmployment.getEmploymentStatus());
        assertEquals("8476975937", clientEmployment.getEmployerInn());
        assertEquals(BigDecimal.valueOf(30000), clientEmployment.getSalary());
        assertEquals(WORKER, clientEmployment.getPosition());
        assertEquals(60, clientEmployment.getWorkExperienceTotal());
        assertEquals(60, clientEmployment.getWorkExperienceCurrent());
        ArgumentCaptor<Credit> creditCaptor = ArgumentCaptor.forClass(Credit.class);
        verify(creditRepository).save(creditCaptor.capture());
        Credit createdCredit = creditCaptor.getValue();
        assertNotNull(createdCredit.getCreditId());
        assertEquals(BigDecimal.valueOf(100000), createdCredit.getAmount());
        assertEquals(36, createdCredit.getTerm());
        assertEquals(BigDecimal.valueOf(10000), createdCredit.getMonthlyPayment());
        assertEquals(BigDecimal.valueOf(15), createdCredit.getRate());
        assertEquals(BigDecimal.valueOf(120000), createdCredit.getPsk());
        assertEquals(3, createdCredit.getPaymentSchedule().size());
        assertEquals(true, createdCredit.getInsuranceEnabled());
        assertEquals(false, createdCredit.getSalaryClient());
        assertEquals(CALCULATED, createdCredit.getCreditStatus());
        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository).save(statementCaptor.capture());
        Statement updatedStatement = statementCaptor.getValue();
        assertEquals(CC_APPROVED, updatedStatement.getStatus());
        assertEquals(createdCredit.getCreditId(), updatedStatement.getCredit().getCreditId());
        StatementStatusHistoryDto statusHistoryElement = updatedStatement.getStatusHistory().getLast();
        assertNotNull(statusHistoryElement.getTime());
        assertEquals("CC_APPROVED", statusHistoryElement.getStatus());
        assertEquals("AUTOMATIC", statusHistoryElement.getChangeType());
    }

    @Test
    public void finishRegistration_SetsStatusDENIED_WhenCalculatorServiceReturns500AndCreditDenied() throws JsonProcessingException {
        UUID statementId = UUID.randomUUID();
        EmploymentDto employment = EmploymentDto.builder()
                .employmentStatus(String.valueOf(EMPLOYED))
                .employerINN("8476975937")
                .salary(BigDecimal.valueOf(30000))
                .position(String.valueOf(WORKER))
                .workExperienceTotal(60)
                .workExperienceCurrent(60)
                .build();
        FinishRegistrationRequestDto finishRegistrationRequestDto = FinishRegistrationRequestDto.builder()
                .gender(String.valueOf(FEMALE))
                .maritalStatus(String.valueOf(SINGLE))
                .dependentAmount(0)
                .passportIssueDate(LocalDate.parse("1930-03-01"))
                .passportIssueBranch("Rusty Lake")
                .employment(employment)
                .accountNumber("098768394578")
                .build();
        Client client = Client.builder()
                .passport(new Passport())
                .build();
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        Statement statement = Statement.builder()
                .client(client)
                .status(APPROVED)
                .appliedOffer(new LoanOfferDto())
                .statusHistory(statusHistory)
                .build();
        when(statementRepository.getReferenceById(statementId)).thenReturn(statement);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ErrorDto errorDto = ErrorDto.builder()
                .denialReason("Age is more than 70")
                .build();
        ObjectMapper objectMapper = new ObjectMapper();
        byte[] errorDtoBytes = objectMapper.writeValueAsBytes(errorDto);
        HttpServerErrorException exception = new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                errorDtoBytes,
                StandardCharsets.UTF_8
        ) {
            @Override
            public <T> T getResponseBodyAs(Class<T> type) {
                try {
                    return objectMapper.readValue(errorDtoBytes, type);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception);
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(kafkaProducer).sendMessage(any(EmailMessage.class), anyString());

        finishRegistrationService.finishRegistration(finishRegistrationRequestDto, String.valueOf(statementId));

        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
        verify(statementRepository).save(statementCaptor.capture());
        Statement updatedStatement = statementCaptor.getValue();
        assertEquals(CC_DENIED, updatedStatement.getStatus());
        StatementStatusHistoryDto statusHistoryElement = updatedStatement.getStatusHistory().getLast();
        assertNotNull(statusHistoryElement.getTime());
        assertEquals("CC_DENIED", statusHistoryElement.getStatus());
        assertEquals("AUTOMATIC", statusHistoryElement.getChangeType());
    }

    @Test
    public void finishRegistration_UpdatesDataAndCreatesCredit_ThrowsCalculatorServiceException_WhenCalculatorServiceReturnsStatus503() {
        UUID statementId = UUID.randomUUID();
        EmploymentDto employment = EmploymentDto.builder()
                .employmentStatus(String.valueOf(EMPLOYED))
                .employerINN("8476975937")
                .salary(BigDecimal.valueOf(30000))
                .position(String.valueOf(WORKER))
                .workExperienceTotal(60)
                .workExperienceCurrent(60)
                .build();
        FinishRegistrationRequestDto finishRegistrationRequestDto = FinishRegistrationRequestDto.builder()
                .gender(String.valueOf(FEMALE))
                .maritalStatus(String.valueOf(SINGLE))
                .dependentAmount(0)
                .passportIssueDate(LocalDate.parse("1930-03-01"))
                .passportIssueBranch("Rusty Lake")
                .employment(employment)
                .accountNumber("098768394578")
                .build();
        Client client = Client.builder()
                .passport(new Passport())
                .build();
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        Statement statement = Statement.builder()
                .client(client)
                .status(APPROVED)
                .appliedOffer(new LoanOfferDto())
                .statusHistory(statusHistory)
                .build();
        when(statementRepository.getReferenceById(statementId)).thenReturn(statement);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception);

        assertThrows(CalculatorServiceException.class,
                () -> finishRegistrationService.finishRegistration(finishRegistrationRequestDto, String.valueOf(statementId)));
    }

    @Test
    public void finishRegistration_ThrowsStatementNotFoundException_WhenStatementDoesNotExist() {
        when(statementRepository.getReferenceById(UUID.randomUUID())).thenReturn(null);

        assertThrows(StatementNotFoundException.class,
                () -> finishRegistrationService.finishRegistration(new FinishRegistrationRequestDto(), UUID.randomUUID().toString()));
    }
}

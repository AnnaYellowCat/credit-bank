package com.neoflex.dealservice.services;

import com.neoflex.dealservice.dto.EmailMessage;
import com.neoflex.dealservice.dto.LoanOfferDto;
import com.neoflex.dealservice.entities.Client;
import com.neoflex.dealservice.entities.Statement;
import com.neoflex.dealservice.exceptions.StatementNotFoundException;
import com.neoflex.dealservice.producers.KafkaProducer;
import com.neoflex.dealservice.repositories.StatementRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.neoflex.dealservice.enums.ApplicationStatus.APPROVED;
import static com.neoflex.dealservice.enums.ApplicationStatus.PREAPPROVAL;
import static com.neoflex.dealservice.enums.ChangeType.AUTOMATIC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class SelectOfferServiceTests {
    @Autowired
    private SelectOfferService selectOfferService;

    @MockitoBean
    private StatementRepository statementRepository;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @Test
    public void selectOffer_UpdatesStatement_WhenStatementExists() {
        UUID statementId = UUID.randomUUID();
        LoanOfferDto offer = LoanOfferDto.builder()
                .statementId(statementId)
                .requestedAmount(new BigDecimal("100000"))
                .term(36)
                .monthlyPayment(new BigDecimal("10000"))
                .rate(BigDecimal.valueOf(15))
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .build();
        List<StatementStatusHistoryDto> statusHistory = new ArrayList<>();
        Client client = Client.builder().email("rose@mail.ru").build();
        when(statementRepository.getReferenceById(statementId)).thenReturn(Statement.builder()
                .statementId(statementId)
                .client(client)
                .status(PREAPPROVAL)
                .statusHistory(statusHistory)
                .build());
        when(statementRepository.save(any(Statement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Statement> statementCaptor = ArgumentCaptor.forClass(Statement.class);
        doNothing().when(kafkaProducer).sendMessage(any(EmailMessage.class), anyString());

        selectOfferService.selectOffer(offer);

        verify(statementRepository).save(statementCaptor.capture());
        Statement statement = statementCaptor.getValue();
        assertThat(offer).usingRecursiveComparison().isEqualTo(statement.getAppliedOffer());
        assertEquals(APPROVED, statement.getStatus());
        StatementStatusHistoryDto statusHistoryElement = statement.getStatusHistory().getLast();
        assertEquals(APPROVED, statusHistoryElement.getStatus());
        assertNotNull(statusHistoryElement.getTime());
        assertEquals(AUTOMATIC, statusHistoryElement.getChangeType());
    }

    @Test
    public void selectOffer_ThrowsStatementNotFoundException_WhenStatementDoesNotExist() {
        when(statementRepository.getReferenceById(UUID.randomUUID())).thenReturn(null);

        assertThrows(StatementNotFoundException.class,
                () -> selectOfferService.selectOffer(new LoanOfferDto()));
    }
}

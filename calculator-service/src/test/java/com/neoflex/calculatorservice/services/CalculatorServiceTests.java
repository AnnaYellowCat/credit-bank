package com.neoflex.calculatorservice.services;

import com.neoflex.calculatorservice.dto.*;
import com.neoflex.calculatorservice.exceptions.LoanDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static com.neoflex.calculatorservice.enums.EmploymentStatus.*;
import static com.neoflex.calculatorservice.enums.MaritalStatus.*;
import static com.neoflex.calculatorservice.enums.Position.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        properties = {
                "insurance.percentage=2.5",
                "salary.minimum=22000",
                "rate.base=15",
                "rate.adjustment.minimal=1",
                "rate.adjustment.small=2",
                "rate.adjustment.medium=3",
                "rate.adjustment.big=5"
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CalculatorServiceTests {

    @Autowired
    private CalculatorService calculatorService;

    @Test
    void getLoanOffers_Returns4LoanOffers(){
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(24)
                .build();

        List<LoanOfferDto> result = calculatorService.getLoanOffers(loanStatementRequestDto);

        assertEquals(4, result.size());
    }

    @Test
    void getLoanOffers_SortsLoanOffersCorrectly(){
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000))
                .term(24)
                .build();

        List<LoanOfferDto> result = calculatorService.getLoanOffers(loanStatementRequestDto);

        for (int i = 0; i < result.size()-1; i++) {
            assertTrue(result.get(i).getTotalAmount()
                    .compareTo(result.get(i+1).getTotalAmount()) >= 0);
        }
    }

    @Test
    void getLoanOffers_ReturnsCorrectLoanOffers_WhenInputValuesAreMinimal(){
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(10000))
                .term(3)
                .build();

        List<LoanOfferDto> result = calculatorService.getLoanOffers(loanStatementRequestDto);

        assertOffer(result.get(0), BigDecimal.valueOf(10524),
                BigDecimal.valueOf(3508), BigDecimal.valueOf(16),
                true, false);
        assertOffer(result.get(1), BigDecimal.valueOf(10455),
                BigDecimal.valueOf(3485), BigDecimal.valueOf(12),
                true, true);
        assertOffer(result.get(2), BigDecimal.valueOf(10335),
                BigDecimal.valueOf(3445), BigDecimal.valueOf(20),
                false, false);
        assertOffer(result.get(3), BigDecimal.valueOf(10234),
                BigDecimal.valueOf(3411), BigDecimal.valueOf(14),
                false, true);
    }

    @Test
    void getLoanOffers_ReturnsCorrectLoanOffers_WhenInputValuesAreMaximum(){
        LoanStatementRequestDto loanStatementRequestDto = LoanStatementRequestDto.builder()
                .amount(BigDecimal.valueOf(100000000))
                .term(360)
                .build();

        List<LoanOfferDto> result = calculatorService.getLoanOffers(loanStatementRequestDto);

        assertOffer(result.get(0), BigDecimal.valueOf(601566727),
                BigDecimal.valueOf(1671018), BigDecimal.valueOf(20),
                false, false);
        assertOffer(result.get(1), BigDecimal.valueOf(496215331),
                BigDecimal.valueOf(1378375), BigDecimal.valueOf(16),
                true, false);
        assertOffer(result.get(2), BigDecimal.valueOf(426553830),
                BigDecimal.valueOf(1184871), BigDecimal.valueOf(14),
                false, true);
        assertOffer(result.get(3), BigDecimal.valueOf(379558048),
                BigDecimal.valueOf(1054327), BigDecimal.valueOf(12),
                true, true);
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenAgeIsMoreThan70(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("1950-01-01"))
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenUnemployed(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .employment(EmploymentDto.builder()
                        .employmentStatus(UNEMPLOYED)
                        .salary(BigDecimal.valueOf(100000))
                        .build())
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenSalaryIsLessThanMinimum(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .salary(BigDecimal.valueOf(10000))
                        .build())
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenTotalWorkExperienceLessThan12(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .workExperienceTotal(1)
                        .salary(BigDecimal.valueOf(25000))
                        .build())
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenCurrentWorkExperienceLessThan3(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .workExperienceTotal(12)
                        .workExperienceCurrent(1)
                        .salary(BigDecimal.valueOf(25000))
                        .build())
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ThrowsLoanDeniedException_WhenSalaryIsTooSmall(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .amount(BigDecimal.valueOf(100000000))
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .workExperienceTotal(36)
                        .workExperienceCurrent(36)
                        .salary(BigDecimal.valueOf(25000))
                        .build())
                .build();

        assertThrows(LoanDeniedException.class,
                () -> calculatorService.getCredit(scoringDataDto));
    }

    @Test
    void getCredit_ReturnsCorrectCreditData_WhenInputValuesAreMinimal(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("2000-01-01"))
                .amount(BigDecimal.valueOf(10000))
                .term(3)
                .maritalStatus(SINGLE)
                .dependentAmount(0)
                .isInsuranceEnabled(false)
                .isSalaryClient(false)
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .position(WORKER)
                        .workExperienceTotal(36)
                        .workExperienceCurrent(36)
                        .salary(BigDecimal.valueOf(25000))
                        .build())
                .build();

        CreditDto creditDto = calculatorService.getCredit(scoringDataDto);

        assertEquals(BigDecimal.valueOf(3461), creditDto.getMonthlyPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(BigDecimal.valueOf(10385), creditDto.getPsk().setScale(0, RoundingMode.DOWN));
        assertEquals(3, creditDto.getPaymentSchedule().size());
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(0), LocalDate.now().plusMonths(1),
                BigDecimal.valueOf(3461), BigDecimal.valueOf(191), BigDecimal.valueOf(3270), BigDecimal.valueOf(6729));
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(1), LocalDate.now().plusMonths(2),
                BigDecimal.valueOf(3461), BigDecimal.valueOf(128), BigDecimal.valueOf(3332), BigDecimal.valueOf(3396));
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(2), LocalDate.now().plusMonths(3),
                BigDecimal.valueOf(3461), BigDecimal.valueOf(65), BigDecimal.valueOf(3396), BigDecimal.valueOf(0));
    }

    @Test
    void getCredit_ReturnsCorrectCreditData_WhenInputValuesAreMaximal(){
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("1980-01-01"))
                .amount(BigDecimal.valueOf(100000000))
                .term(360)
                .maritalStatus(MARRIED)
                .dependentAmount(2)
                .isInsuranceEnabled(true)
                .isSalaryClient(true)
                .employment(EmploymentDto.builder()
                        .employmentStatus(BUSINESS_OWNER)
                        .position(OWNER)
                        .workExperienceTotal(240)
                        .workExperienceCurrent(240)
                        .salary(BigDecimal.valueOf(20000000))
                        .build())
                .build();

        CreditDto creditDto = calculatorService.getCredit(scoringDataDto);

        assertEquals(BigDecimal.valueOf(824738), creditDto.getMonthlyPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(BigDecimal.valueOf(296905745), creditDto.getPsk().setScale(0, RoundingMode.DOWN));
        assertEquals(360, creditDto.getPaymentSchedule().size());
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(0), LocalDate.now().plusMonths(1),
                BigDecimal.valueOf(824738), BigDecimal.valueOf(768750), BigDecimal.valueOf(55988), BigDecimal.valueOf(102444011));
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(179), LocalDate.now().plusMonths(180),
                BigDecimal.valueOf(824738), BigDecimal.valueOf(611452), BigDecimal.valueOf(213285), BigDecimal.valueOf(81313748));
        assertPaymentScheduleElement(creditDto.getPaymentSchedule().get(359), LocalDate.now().plusMonths(360),
                BigDecimal.valueOf(824738), BigDecimal.valueOf(6139), BigDecimal.valueOf(818598), BigDecimal.valueOf(0));
    }

    @Test
    void getCredit_ReturnsCorrectCreditData_WhenNoInsuranceAndSalaryClient() {
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("1990-01-01"))
                .amount(BigDecimal.valueOf(100000))
                .term(36)
                .maritalStatus(WIDOW_WIDOWER)
                .dependentAmount(1)
                .isInsuranceEnabled(false)
                .isSalaryClient(true)
                .employment(EmploymentDto.builder()
                        .employmentStatus(SELF_EMPLOYED)
                        .position(TOP_MANAGER)
                        .workExperienceTotal(60)
                        .workExperienceCurrent(60)
                        .salary(BigDecimal.valueOf(60000))
                        .build())
                .build();

        CreditDto creditDto = calculatorService.getCredit(scoringDataDto);

        assertEquals(BigDecimal.valueOf(3515), creditDto.getMonthlyPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(BigDecimal.valueOf(126565), creditDto.getPsk().setScale(0, RoundingMode.DOWN));
        assertEquals(36, creditDto.getPaymentSchedule().size());
    }

    @Test
    void getCredit_ReturnsCorrectCreditData_WhenInsuranceAndNotSalaryClient() {
        ScoringDataDto scoringDataDto = ScoringDataDto.builder()
                .birthdate(LocalDate.parse("1990-01-01"))
                .amount(BigDecimal.valueOf(50000))
                .term(24)
                .maritalStatus(DIVORCED)
                .dependentAmount(0)
                .isInsuranceEnabled(true)
                .isSalaryClient(false)
                .employment(EmploymentDto.builder()
                        .employmentStatus(EMPLOYED)
                        .position(MID_MANAGER)
                        .workExperienceTotal(60)
                        .workExperienceCurrent(60)
                        .salary(BigDecimal.valueOf(50000))
                        .build())
                .build();

        CreditDto creditDto = calculatorService.getCredit(scoringDataDto);

        assertEquals(BigDecimal.valueOf(2608), creditDto.getMonthlyPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(BigDecimal.valueOf(62601), creditDto.getPsk().setScale(0, RoundingMode.DOWN));
        assertEquals(24, creditDto.getPaymentSchedule().size());
    }

    private void assertOffer(LoanOfferDto offer,
                             BigDecimal expectedTotalAmount,
                             BigDecimal expectedMonthlyPayment,
                             BigDecimal expectedRate,
                             boolean isInsuranceEnabled,
                             boolean isSalaryClient) {
        assertEquals(expectedTotalAmount, offer.getTotalAmount().setScale(0, RoundingMode.DOWN));
        assertEquals(expectedMonthlyPayment, offer.getMonthlyPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(expectedRate, offer.getRate().setScale(0, RoundingMode.DOWN));
        assertEquals(isInsuranceEnabled, offer.getIsInsuranceEnabled());
        assertEquals(isSalaryClient, offer.getIsSalaryClient());
    }

    private void assertPaymentScheduleElement(PaymentScheduleElementDto element, LocalDate expectedDate,
                                              BigDecimal expectedTotalPayment, BigDecimal expectedInterestPayment,
                                              BigDecimal expectedDebtPayment, BigDecimal expectedRemainingDebt) {
        assertEquals(expectedDate, element.getDate());
        assertEquals(expectedTotalPayment, element.getTotalPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(expectedInterestPayment, element.getInterestPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(expectedDebtPayment, element.getDebtPayment().setScale(0, RoundingMode.DOWN));
        assertEquals(expectedRemainingDebt, element.getRemainingDebt().setScale(0, RoundingMode.DOWN));
    }
}

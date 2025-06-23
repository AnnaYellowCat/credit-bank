package com.neoflex.calculatorservice.services;

import com.neoflex.calculatorservice.dto.*;
import com.neoflex.calculatorservice.enums.EmploymentStatus;
import com.neoflex.calculatorservice.enums.MaritalStatus;
import com.neoflex.calculatorservice.enums.Position;
import com.neoflex.calculatorservice.exceptions.LoanDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.neoflex.calculatorservice.enums.EmploymentStatus.*;
import static com.neoflex.calculatorservice.enums.MaritalStatus.*;
import static com.neoflex.calculatorservice.enums.Position.*;

@Slf4j
@Service
public class CalculatorService {
    @Value("${insurance.percentage}")
    private BigDecimal insurancePercentage;

    @Value("${salary.minimum}")
    private BigDecimal minimumSalary;

    @Value("${salary.months.number}")
    private int salaryMonthsNumber;

    @Value("${age.maximum}")
    private int maximumAge;

    @Value("${experience.minimum.total}")
    private int minimumExperienceTotal;

    @Value("${experience.minimum.current}")
    private int minimumExperienceCurrent;

    @Value("${rate.base}")
    private BigDecimal baseRate;

    @Value("${rate.adjustment.minimum}")
    private BigDecimal minimumRateAdjustment;

    @Value("${rate.adjustment.small}")
    private BigDecimal smallRateAdjustment;

    @Value("${rate.adjustment.medium}")
    private BigDecimal mediumRateAdjustment;

    @Value("${rate.adjustment.big}")
    private BigDecimal bigRateAdjustment;

    private static final int TWELVE_MONTHS = 12;
    private static final int ONE_HUNDRED_PERCENT = 100;
    private static final int LOAN_OFFERS_NUMBER = 4;
    private static final BigDecimal ONE = BigDecimal.valueOf(1);
    private static final int ROUNDING_SCALE = 2;
    private static final int ROUNDING_CALCULATING_SCALE = 11;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public List<LoanOfferDto> getLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        List<LoanOfferDto> loanOffers = new ArrayList<>(LOAN_OFFERS_NUMBER);

        log.info("Creating offer 1: non-salary client, no insurance");
        loanOffers.add(createLoanOfferDto(loanStatementRequestDto, false, false));

        log.info("Creating offer 2: non-salary client, with insurance");
        loanOffers.add(createLoanOfferDto(loanStatementRequestDto, true, false));

        log.info("Creating offer 3: salary client, no insurance");
        loanOffers.add(createLoanOfferDto(loanStatementRequestDto, false, true));

        log.info("Creating offer 4: salary client, with insurance");
        loanOffers.add(createLoanOfferDto(loanStatementRequestDto, true, true));

        // Sort offers from worst to best
        loanOffers.sort((o1, o2) -> o2.getTotalAmount()
                .compareTo(o1.getTotalAmount()));
        return loanOffers;
    }

    private LoanOfferDto createLoanOfferDto(LoanStatementRequestDto loanStatementRequestDto,
                                            boolean isInsuranceEnabled, boolean isSalaryClient) {
        LoanOfferDto offer = new LoanOfferDto();
        offer.setTerm(loanStatementRequestDto.getTerm());
        offer.setRequestedAmount(loanStatementRequestDto.getAmount());
        offer.setStatementId(UUID.randomUUID());
        offer.setIsInsuranceEnabled(isInsuranceEnabled);
        offer.setIsSalaryClient(isSalaryClient);
        offer.setRate(calculateRate(isInsuranceEnabled, isSalaryClient));

        if(isInsuranceEnabled){
            offer.setMonthlyPayment(calculateMonthPayment(
                    addInsurancePrice(offer.getRequestedAmount()),
                    offer.getTerm(), offer.getRate()));
        }
        else{
            offer.setMonthlyPayment(calculateMonthPayment(offer.getRequestedAmount(),
                    offer.getTerm(), offer.getRate()));
        }

        offer.setTotalAmount(offer.getMonthlyPayment()
                .multiply(BigDecimal.valueOf(offer.getTerm())));

        return offer;
    }

    public CreditDto getCredit(ScoringDataDto scoringDataDto) {
        log.info("Checking if it is available to issue a loan");

        checkAge(scoringDataDto.getBirthDate());

        BigDecimal salary = scoringDataDto.getEmployment().getSalary();
        EmploymentStatus employmentStatus = scoringDataDto.getEmployment().getEmploymentStatus();
        Integer workExperienceTotal = scoringDataDto.getEmployment().getWorkExperienceTotal();
        Integer workExperienceCurrent = scoringDataDto.getEmployment().getWorkExperienceCurrent();
        Position position = scoringDataDto.getEmployment().getPosition();
        MaritalStatus maritalStatus = scoringDataDto.getMaritalStatus();

        checkEmployment(employmentStatus, salary, workExperienceTotal, workExperienceCurrent);

        checkPaymentAbility(scoringDataDto.getAmount(), salary);

        log.info("Setting already known fields");
        CreditDto creditDto = new CreditDto();
        creditDto.setAmount(scoringDataDto.getAmount());
        creditDto.setTerm(scoringDataDto.getTerm());
        creditDto.setIsInsuranceEnabled(scoringDataDto.getIsInsuranceEnabled());
        creditDto.setIsSalaryClient(scoringDataDto.getIsSalaryClient());

        log.info("Calculating rate");
        creditDto.setRate(calculateRate(scoringDataDto.getIsInsuranceEnabled(), scoringDataDto.getIsSalaryClient()));

        if(maritalStatus.equals(SINGLE)) {
            creditDto.setRate(creditDto.getRate()
                    .add(mediumRateAdjustment));
        }
        if(maritalStatus.equals(MARRIED)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(mediumRateAdjustment));
        }
        if(maritalStatus.equals(DIVORCED)) {
            creditDto.setRate(creditDto.getRate()
                    .add(bigRateAdjustment));
        }

        creditDto.setRate(creditDto.getRate()
                .add(minimumRateAdjustment.multiply(BigDecimal.valueOf(scoringDataDto.getDependentAmount()))));

        if(employmentStatus.equals(SELF_EMPLOYED)) {
            creditDto.setRate(creditDto.getRate()
                    .add(mediumRateAdjustment));
        }
        if(employmentStatus.equals(BUSINESS_OWNER)) {
            creditDto.setRate(creditDto.getRate()
                    .add(minimumRateAdjustment));
        }

        if(position.equals(MID_MANAGER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(minimumRateAdjustment));
        }
        if(position.equals(TOP_MANAGER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(smallRateAdjustment));
        }
        if(position.equals(OWNER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(mediumRateAdjustment));
        }

        log.info("Final calculated rate: {}", creditDto.getRate().setScale(ROUNDING_SCALE, ROUNDING_MODE));

        if(creditDto.getIsInsuranceEnabled()){
            creditDto.setMonthlyPayment(calculateMonthPayment(
                    addInsurancePrice(creditDto.getAmount()),
                    creditDto.getTerm(), creditDto.getRate()));
        }
        else{
            creditDto.setMonthlyPayment(calculateMonthPayment(creditDto.getAmount(),
                    creditDto.getTerm(), creditDto.getRate()));
        }
        log.info("Calculated monthly payment: {}", creditDto.getMonthlyPayment().setScale(ROUNDING_SCALE, ROUNDING_MODE));

        creditDto.setPsk(creditDto.getMonthlyPayment()
                .multiply(BigDecimal.valueOf(creditDto.getTerm())));
        log.info("Calculated psk: {}", creditDto.getPsk().setScale(ROUNDING_SCALE, ROUNDING_MODE));

        creditDto.setPaymentSchedule(calculatePaymentSchedule(creditDto));
        log.info("Calculated payment schedule with {} elements", creditDto.getPaymentSchedule().size());

        return creditDto;
    }

    private BigDecimal calculateRate(boolean isInsuranceEnabled, boolean isSalaryClient) {
        if (!isInsuranceEnabled && !isSalaryClient) {
            return baseRate.add(bigRateAdjustment);
        }
        if (isInsuranceEnabled && !isSalaryClient) {
            return baseRate.add(minimumRateAdjustment);
        }
        if (!isInsuranceEnabled && isSalaryClient) {
            return baseRate.subtract(minimumRateAdjustment);
        }
        return baseRate.subtract(mediumRateAdjustment);
    }

    private BigDecimal calculateMonthPayment(BigDecimal requestedAmount, Integer numberOfMonths,
                                             BigDecimal rate) {
        BigDecimal monthlyInterestRate = rate
                .divide(BigDecimal.valueOf(ONE_HUNDRED_PERCENT*TWELVE_MONTHS), ROUNDING_CALCULATING_SCALE, ROUNDING_MODE);

        // Calculate annuity factor according to the formula
        // (М * (1 + М) ^ S) / ((1 + М) ^ S — 1), M - monthly interest rate, S - number of months
        BigDecimal auxiliary = (monthlyInterestRate
                .add(ONE))
                .pow(numberOfMonths);
        BigDecimal annuityFactor = monthlyInterestRate
                .multiply(auxiliary)
                .divide((auxiliary)
                        .subtract(ONE), ROUNDING_CALCULATING_SCALE, ROUNDING_MODE);

        return requestedAmount.multiply(annuityFactor);
    }

    private BigDecimal addInsurancePrice(BigDecimal amount){
        return amount.add(insurancePercentage.multiply(amount
                .divide(BigDecimal.valueOf(ONE_HUNDRED_PERCENT), ROUNDING_CALCULATING_SCALE, ROUNDING_MODE)));
    }

    private void checkAge(LocalDate birthdate) {
        LocalDate today = LocalDate.now();
        Period age = Period.between(birthdate, today);
        if (age.getYears() > maximumAge) {
            log.info("Loan denied: Client age {} is more than {} years", age.getYears(), maximumAge);
            throw new LoanDeniedException("Age more than " + maximumAge + " years");
        }
    }

    private void checkEmployment(EmploymentStatus status, BigDecimal salary,
                                 Integer workExperienceTotal,  Integer workExperienceCurrent){
        if (status.equals(UNEMPLOYED) || (salary.compareTo(minimumSalary) < 0) ||
                workExperienceTotal < minimumExperienceTotal || workExperienceCurrent < minimumExperienceCurrent
        ) {
            log.info("Loan denied: inappropriate employment criteria - status: {}, salary: {}, total experience: {} months, current experience: {} months",
                    status, salary.setScale(ROUNDING_SCALE, ROUNDING_MODE),
                    workExperienceTotal, workExperienceCurrent);
            throw new LoanDeniedException("Work experience less than " + minimumExperienceTotal + " months");
        }
    }

    private void checkPaymentAbility(BigDecimal loanAmount, BigDecimal salary) {
        if (loanAmount
                .compareTo(salary.multiply(BigDecimal.valueOf(salaryMonthsNumber))) > 0) {
            log.info("Loan denied: Requested amount {} exceeds {} months salary {}",
                    loanAmount, salaryMonthsNumber, salary
                            .multiply(BigDecimal.valueOf(salaryMonthsNumber)).setScale(ROUNDING_SCALE, ROUNDING_MODE));
            throw new LoanDeniedException("The loan amount exceeds income for " + salaryMonthsNumber + " months");
        }
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(CreditDto creditDto) {
        List<PaymentScheduleElementDto> scheduleElements = new ArrayList<>(creditDto.getTerm());

        BigDecimal monthlyInterestRate = creditDto.getRate()
                .divide(BigDecimal.valueOf(ONE_HUNDRED_PERCENT*TWELVE_MONTHS), ROUNDING_CALCULATING_SCALE, ROUNDING_MODE);

        BigDecimal remainingDebt;
        if(creditDto.getIsInsuranceEnabled()){
            remainingDebt = addInsurancePrice(creditDto.getAmount());
        }
        else{
            remainingDebt = creditDto.getAmount();
        }

        for (int i = 0; i < creditDto.getTerm(); i++) {
            PaymentScheduleElementDto scheduleElement = new PaymentScheduleElementDto();

            scheduleElement.setNumber(i+1);
            scheduleElement.setDate(LocalDate.now().plusMonths(i + 1));
            scheduleElement.setTotalPayment(creditDto.getMonthlyPayment());

            scheduleElement.setInterestPayment(remainingDebt
                    .multiply(monthlyInterestRate));

            scheduleElement.setDebtPayment(scheduleElement.getTotalPayment()
                        .subtract(scheduleElement.getInterestPayment()));

            remainingDebt = remainingDebt.subtract(scheduleElement.getDebtPayment());
            scheduleElement.setRemainingDebt(remainingDebt);

            scheduleElements.add(scheduleElement);
        }

        return scheduleElements;
    }
}

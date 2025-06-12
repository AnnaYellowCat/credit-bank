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
    @Value("${rate.base}")
    private BigDecimal baseRate;

    @Value("${insurance.percentage}")
    private BigDecimal insurancePercentage;

    @Value("${salary.minimum}")
    private BigDecimal minimumSalary;

    @Value("${rate.adjustment.minimal}")
    private BigDecimal minimalRateAdjustment;

    @Value("${rate.adjustment.small}")
    private BigDecimal smallRateAdjustment;

    @Value("${rate.adjustment.medium}")
    private BigDecimal mediumRateAdjustment;

    @Value("${rate.adjustment.big}")
    private BigDecimal bigRateAdjustment;

    public List<LoanOfferDto> getLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        List<LoanOfferDto> loanOffers = new ArrayList<>(4);

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

        checkAge(scoringDataDto.getBirthdate());

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
                .add(minimalRateAdjustment.multiply(BigDecimal.valueOf(scoringDataDto.getDependentAmount()))));

        if(employmentStatus.equals(SELF_EMPLOYED)) {
            creditDto.setRate(creditDto.getRate()
                    .add(mediumRateAdjustment));
        }
        if(employmentStatus.equals(BUSINESS_OWNER)) {
            creditDto.setRate(creditDto.getRate()
                    .add(minimalRateAdjustment));
        }

        if(position.equals(MID_MANAGER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(minimalRateAdjustment));
        }
        if(position.equals(TOP_MANAGER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(smallRateAdjustment));
        }
        if(position.equals(OWNER)) {
            creditDto.setRate(creditDto.getRate()
                    .subtract(mediumRateAdjustment));
        }

        log.info("Final calculated rate: {}", creditDto.getRate().setScale(2, RoundingMode.HALF_EVEN));

        if(creditDto.getIsInsuranceEnabled()){
            creditDto.setMonthlyPayment(calculateMonthPayment(
                    addInsurancePrice(creditDto.getAmount()),
                    creditDto.getTerm(), creditDto.getRate()));
        }
        else{
            creditDto.setMonthlyPayment(calculateMonthPayment(creditDto.getAmount(),
                    creditDto.getTerm(), creditDto.getRate()));
        }
        log.info("Calculated monthly payment: {}", creditDto.getMonthlyPayment().setScale(2, RoundingMode.HALF_EVEN));

        creditDto.setPsk(creditDto.getMonthlyPayment()
                .multiply(BigDecimal.valueOf(creditDto.getTerm())));
        log.info("Calculated psk: {}", creditDto.getPsk().setScale(2, RoundingMode.HALF_EVEN));

        creditDto.setPaymentSchedule(calculatePaymentSchedule(creditDto));
        log.info("Calculated payment schedule with {} elements", creditDto.getPaymentSchedule().size());

        return creditDto;
    }

    private BigDecimal calculateRate(boolean isInsuranceEnabled, boolean isSalaryClient) {
        if (!isInsuranceEnabled && !isSalaryClient) {
            return baseRate.add(bigRateAdjustment);
        }
        if (isInsuranceEnabled && !isSalaryClient) {
            return baseRate.add(minimalRateAdjustment);
        }
        if (!isInsuranceEnabled && isSalaryClient) {
            return baseRate.subtract(minimalRateAdjustment);
        }
        return baseRate.subtract(mediumRateAdjustment);
    }

    private BigDecimal calculateMonthPayment(BigDecimal requestedAmount, Integer numberOfMonths,
                                             BigDecimal rate) {
        BigDecimal monthlyInterestRate = rate.divide(BigDecimal.valueOf(100*12), 11, RoundingMode.HALF_EVEN);

        // Calculate annuity factor according to the formula
        // (М * (1 + М) ^ S) / ((1 + М) ^ S — 1), M - monthly interest rate, S - number of months
        BigDecimal auxiliary = (monthlyInterestRate
                .add(BigDecimal.valueOf(1)))
                .pow(numberOfMonths);
        BigDecimal annuityFactor = monthlyInterestRate
                .multiply(auxiliary)
                .divide((auxiliary)
                        .subtract(BigDecimal.valueOf(1)), 11, RoundingMode.HALF_EVEN);

        return requestedAmount.multiply(annuityFactor);
    }

    private BigDecimal addInsurancePrice(BigDecimal amount){
        return amount.add(insurancePercentage.multiply(amount
                .divide(BigDecimal.valueOf(100), 11, RoundingMode.HALF_EVEN)));
    }

    private void checkAge(LocalDate birthdate) {
        LocalDate today = LocalDate.now();
        Period age = Period.between(birthdate, today);
        if (age.getYears() > 70) {
            log.info("Loan denied: Client age {} is more than 70 years", age.getYears());
            throw new LoanDeniedException("Age more than 70 years");
        }
    }

    private void checkEmployment(EmploymentStatus status, BigDecimal salary,
                                 Integer workExperienceTotal,  Integer workExperienceCurrent){
        if (status.equals(UNEMPLOYED) || (salary.compareTo(minimumSalary) < 0) ||
                workExperienceTotal < 12 || workExperienceCurrent < 3
        ) {
            log.info("Loan denied: inappropriate employment criteria - status: {}, salary: {}, total experience: {} months, current experience: {} months",
                    status, salary.setScale(2, RoundingMode.HALF_EVEN),
                    workExperienceTotal, workExperienceCurrent);
            throw new LoanDeniedException("Work experience less than 1 year");
        }
    }

    private void checkPaymentAbility(BigDecimal loanAmount, BigDecimal salary) {
        if (loanAmount
                .compareTo(salary.multiply(BigDecimal.valueOf(6))) > 0) {
            log.info("Loan denied: Requested amount {} exceeds 6 months salary {}",
                    loanAmount, salary.multiply(BigDecimal.valueOf(6)).setScale(2, RoundingMode.HALF_EVEN));
            throw new LoanDeniedException("The loan amount exceeds income for 6 months");
        }
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(CreditDto creditDto) {
        List<PaymentScheduleElementDto> scheduleElements = new ArrayList<>(creditDto.getTerm());

        BigDecimal monthlyInterestRate = creditDto.getRate()
                .divide(BigDecimal.valueOf(100*12), 11, RoundingMode.HALF_EVEN);

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

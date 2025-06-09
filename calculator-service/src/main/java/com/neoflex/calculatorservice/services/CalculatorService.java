package com.neoflex.calculatorservice.services;

import com.neoflex.calculatorservice.dto.*;
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

@Slf4j
@Service
public class CalculatorService {
    @Value("${base.rate}")
    private BigDecimal baseRate;

    @Value("${insurance.percentage}")
    private BigDecimal insurancePercentage;

    @Value("${minimum.threshold.salary}")
    private BigDecimal minimumSalary;

    @Value("${minimum.rate.adjustment}")
    private BigDecimal minimumRateAdjustment;

    @Value("${small.rate.adjustment}")
    private BigDecimal smallRateAdjustment;

    @Value("${medium.rate.adjustment}")
    private BigDecimal mediumRateAdjustment;

    @Value("${big.rate.adjustment}")
    private BigDecimal bigRateAdjustment;

    public List<LoanOfferDto> getLoanOffers(LoanStatementRequestDto loanStatementRequestDto) {
        List<LoanOfferDto> loanOffers = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) loanOffers.add(new LoanOfferDto());

        // Set common values for all offers
        for (LoanOfferDto loanOfferDto : loanOffers) {
            loanOfferDto.setTerm(loanStatementRequestDto.getTerm());
            loanOfferDto.setRequestedAmount(loanStatementRequestDto.getAmount());
            loanOfferDto.setStatementId(UUID.randomUUID());
        }

        log.info("Creating offer 1: non-salary client, no insurance");
        loanOffers.get(0).setIsSalaryClient(false);
        loanOffers.get(0).setIsInsuranceEnabled(false);
        // Increase the base rate by 5
        loanOffers.get(0).setRate(baseRate.add(bigRateAdjustment));
        loanOffers.get(0).setMonthlyPayment(calculateMonthPayment(loanOffers.get(0).getRequestedAmount(),
                loanOffers.get(0).getTerm(), loanOffers.get(0).getRate()));

        log.info("Creating offer 2: non-salary client, with insurance");
        loanOffers.get(1).setIsSalaryClient(false);
        loanOffers.get(1).setIsInsuranceEnabled(true);
        // Increase the base rate by 1
        loanOffers.get(1).setRate(baseRate.add(minimumRateAdjustment));
        // Calculate monthly payment with loan amount as requested amount + insurance price
        loanOffers.get(1).setMonthlyPayment(calculateMonthPayment(
                calculateWithInsurancePrice(loanOffers.get(1).getRequestedAmount()),
                loanOffers.get(1).getTerm(), loanOffers.get(1).getRate()));

        log.info("Creating offer 3: salary client, no insurance");
        loanOffers.get(2).setIsSalaryClient(true);
        loanOffers.get(2).setIsInsuranceEnabled(false);
        // Decrease the base rate by 1
        loanOffers.get(2).setRate(baseRate.subtract(minimumRateAdjustment));
        loanOffers.get(2).setMonthlyPayment(calculateMonthPayment(loanOffers.get(2).getRequestedAmount(),
                loanOffers.get(2).getTerm(), loanOffers.get(2).getRate()));

        log.info("Creating offer 4: salary client, with insurance");
        loanOffers.get(3).setIsSalaryClient(true);
        loanOffers.get(3).setIsInsuranceEnabled(true);
        // Decrease the base rate by 3
        loanOffers.get(3).setRate(baseRate.subtract(mediumRateAdjustment));
        // Calculate monthly payment with loan amount as requested amount + insurance price
        loanOffers.get(3).setMonthlyPayment(calculateMonthPayment(
                calculateWithInsurancePrice(loanOffers.get(3).getRequestedAmount()),
                loanOffers.get(3).getTerm(), loanOffers.get(3).getRate()));

        // Calculate and set total amount of credit (monthly payment * number of months)
        for (LoanOfferDto offer : loanOffers) {
            offer.setTotalAmount(offer.getMonthlyPayment()
                    .multiply(BigDecimal.valueOf(offer.getTerm())));
        }

        // Sort offers from worst to best
        loanOffers.sort((o1, o2) -> o2.getTotalAmount()
                .compareTo(o1.getTotalAmount()));
        return loanOffers;
    }

    public CreditDto getCredit(ScoringDataDto scoringDataDto) {
        // Check if it is available to issue a loan
        log.info("Checking if it is available to issue a loan");

        // Check if age is less than 70
        LocalDate today = LocalDate.now();
        Period age = Period.between(scoringDataDto.getBirthdate(), today);
        if (age.getYears() > 70) {
            log.info("Loan denied: Client age {} is more than 70 years", age.getYears());
            throw new LoanDeniedException("Age more than 70 years");
        }

        // Check if client is employed, has salary above minimum threshold,
        // total work experience at least 1 year and current work experience at least 3 months
        if (scoringDataDto.getEmployment().getEmploymentStatus().equals(UNEMPLOYED) ||
                (scoringDataDto.getEmployment().getSalary().compareTo(minimumSalary) < 0) ||
                scoringDataDto.getEmployment().getWorkExperienceTotal() < 12 ||
                scoringDataDto.getEmployment().getWorkExperienceCurrent() < 3
        ) {
            log.info("Loan denied: inappropriate employment criteria - status: {}, salary: {}, total experience: {} months, current experience: {} months",
                    scoringDataDto.getEmployment().getEmploymentStatus(),
                    scoringDataDto.getEmployment().getSalary(),
                    scoringDataDto.getEmployment().getWorkExperienceTotal(),
                    scoringDataDto.getEmployment().getWorkExperienceCurrent());
            throw new LoanDeniedException("Work experience less than 1 year");
        }

        // Check if the requested amount exceeds 50% of annual income
        if (scoringDataDto.getAmount()
                .compareTo(scoringDataDto.getEmployment().getSalary()
                        .multiply(BigDecimal.valueOf(6))) > 0) {
            log.info("Loan denied: Requested amount {} exceeds 6 months salary {}",
                    scoringDataDto.getAmount(), scoringDataDto.getEmployment().getSalary()
                            .multiply(BigDecimal.valueOf(6)));
            throw new LoanDeniedException("The loan amount exceeds income for 6 months");
        }

        // Set already known fields
        log.info("Setting already known fields");
        CreditDto creditDto = new CreditDto();
        creditDto.setAmount(scoringDataDto.getAmount());
        creditDto.setTerm(scoringDataDto.getTerm());
        creditDto.setIsInsuranceEnabled(scoringDataDto.getIsInsuranceEnabled());
        creditDto.setIsSalaryClient(scoringDataDto.getIsSalaryClient());

        // Change the rate depending on the parameters
        log.info("Calculating rate");
        if (scoringDataDto.getIsSalaryClient() == false && scoringDataDto.getIsInsuranceEnabled() == false) {
            creditDto.setRate(baseRate
                    .add(bigRateAdjustment));
        }

        if (scoringDataDto.getIsSalaryClient() == false && scoringDataDto.getIsInsuranceEnabled() == true) {
            creditDto.setRate(baseRate
                    .add(minimumRateAdjustment));
        }

        if (scoringDataDto.getIsSalaryClient() == true && scoringDataDto.getIsInsuranceEnabled() == false) {
            creditDto.setRate(baseRate
                    .subtract(minimumRateAdjustment));
        }

        if (scoringDataDto.getIsSalaryClient() == true && scoringDataDto.getIsInsuranceEnabled() == true) {
            creditDto.setRate(baseRate
                    .subtract(mediumRateAdjustment));
        }

        switch (scoringDataDto.getMaritalStatus()) {
            case MARRIED:
                creditDto.setRate(creditDto.getRate()
                        .subtract(mediumRateAdjustment));
                break;
            case NOT_MARRIED:
                creditDto.setRate(creditDto.getRate()
                        .add(mediumRateAdjustment));
                break;
        }

        // The rate increases by minimum rate adjustment for each dependent
        creditDto.setRate(creditDto.getRate()
                .add(minimumRateAdjustment.multiply(BigDecimal.valueOf(scoringDataDto.getDependentAmount()))));

        switch (scoringDataDto.getEmployment().getEmploymentStatus()) {
            case SELF_EMPLOYED:
                creditDto.setRate(creditDto.getRate()
                        .add(mediumRateAdjustment));
                break;
            case BUSINESS_OWNER:
                creditDto.setRate(creditDto.getRate()
                        .add(minimumRateAdjustment));
                break;
            default:
                break;
        }

        switch (scoringDataDto.getEmployment().getPosition()) {
            case MIDDLE_MANAGER:
                creditDto.setRate(creditDto.getRate()
                        .subtract(minimumRateAdjustment));
                break;
            case TOP_MANAGER:
                creditDto.setRate(creditDto.getRate()
                        .subtract(smallRateAdjustment));
                break;
            case OWNER:
                creditDto.setRate(creditDto.getRate()
                        .subtract(mediumRateAdjustment));
                break;
            default:
                break;
        }

        log.info("Final calculated rate: {}", creditDto.getRate());

        // Calculate monthly payment
        // If insurance is included (loan amount = requested amount + insurance price)
        if(creditDto.getIsInsuranceEnabled()){
            creditDto.setMonthlyPayment(calculateMonthPayment(
                    calculateWithInsurancePrice(creditDto.getAmount()),
                    creditDto.getTerm(), creditDto.getRate()));
            log.info("Calculated monthly payment: {}", creditDto.getMonthlyPayment());
        }
        // If insurance is not included
        else{
            creditDto.setMonthlyPayment(calculateMonthPayment(creditDto.getAmount(),
                    creditDto.getTerm(), creditDto.getRate()));
            log.info("Calculated monthly payment: {}", creditDto.getMonthlyPayment());
        }

        // Calculate psk - requested amount + percentages
        creditDto.setPsk(creditDto.getMonthlyPayment()
                .multiply(BigDecimal.valueOf(creditDto.getTerm())));
        log.info("Calculated psk: {}", creditDto.getPsk());

        // Calculate payment schedule
        creditDto.setPaymentSchedule(calculatePaymentSchedule(creditDto));
        log.info("Calculated payment schedule with {} elements", creditDto.getPaymentSchedule().size());

        return creditDto;
    }

    private BigDecimal calculateWithInsurancePrice(BigDecimal amount){
        return amount.add(insurancePercentage.multiply(amount
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)));
    }

    private BigDecimal calculateMonthPayment(BigDecimal requestedAmount, Integer numberOfMonths, 
                                             BigDecimal rate) {
        // Calculate monthly interest rate on a loan as a decimal fraction
        BigDecimal monthlyInterestRate = rate.divide(BigDecimal.valueOf(100*12), 10, RoundingMode.HALF_EVEN);

        // Calculate annuity factor according to the formula
        // (М * (1 + М) ^ S) / ((1 + М) ^ S — 1), M - monthly interest rate, S - number of months
        BigDecimal auxiliary = (monthlyInterestRate
                .add(BigDecimal.valueOf(1)))
                .pow(numberOfMonths);
        BigDecimal annuityFactor = monthlyInterestRate
                .multiply(auxiliary)
                .divide((auxiliary)
                        .subtract(BigDecimal.valueOf(1)), 10, RoundingMode.HALF_EVEN);

        // Calculate and return month payment
        return requestedAmount.multiply(annuityFactor);
    }

    private List<PaymentScheduleElementDto> calculatePaymentSchedule(CreditDto creditDto) {
        List<PaymentScheduleElementDto> scheduleElements = new ArrayList<>(creditDto.getTerm());

        // Calculate monthly interest rate on a loan as a decimal fraction
        BigDecimal monthlyInterestRate = creditDto.getRate()
                .divide(BigDecimal.valueOf(100*12), 10, RoundingMode.HALF_EVEN);

        BigDecimal remainingDebt;
        // Calculate initial remaining debt
        if(creditDto.getIsInsuranceEnabled()){
            remainingDebt = calculateWithInsurancePrice(creditDto.getAmount());
        }
        else{
            remainingDebt = creditDto.getAmount();
        }


        for (int i = 0; i < creditDto.getTerm(); i++) {
            PaymentScheduleElementDto scheduleElement = new PaymentScheduleElementDto();

            scheduleElement.setNumber(i+1);
            scheduleElement.setDate(LocalDate.now().plusMonths(i + 1));
            scheduleElement.setTotalPayment(creditDto.getMonthlyPayment());

            // Calculate interest payment (remaining debt * monthly interest rate)
            scheduleElement.setInterestPayment(remainingDebt
                    .multiply(monthlyInterestRate));

            // Calculate debt payment (total month payment - interest payment)
            scheduleElement.setDebtPayment(scheduleElement.getTotalPayment()
                        .subtract(scheduleElement.getInterestPayment()));

            // Calculate remaining debt (remaining debt - debt payment)
            remainingDebt = remainingDebt.subtract(scheduleElement.getDebtPayment());
            scheduleElement.setRemainingDebt(remainingDebt);

            scheduleElements.add(scheduleElement);
        }

        return scheduleElements;
    }
}

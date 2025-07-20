package com.neoflex.dossierservice.sevices;

import com.neoflex.dossierservice.dto.CreditDto;
import com.neoflex.dossierservice.dto.EmailMessage;
import com.neoflex.dossierservice.dto.PaymentScheduleElementDto;
import com.neoflex.dossierservice.exceptions.DealServiceException;
import com.neoflex.dossierservice.exceptions.EmailMessageException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    private final static String SPACE = " ";
    private final static String NEW_LINE = "\n";

    @Value("${spring.mail.username}")
    private String senderName;
    @Value("${app.gateway.uri}")
    private String gatewayUri;
    @Value("${app.deal.document.base}")
    private String baseDocumentUrl;
    @Value("${app.deal.document.sign}")
    private String signDocumentUrl;
    @Value("${app.deal.credit}")
    private String getCreditUrl;
    @Value("${email.finish-registration.subject}")
    private String finishRegSubject;
    @Value("${email.finish-registration.text}")
    private String finishRegText;
    @Value("${email.create-documents.subject}")
    private String createDocsSubject;
    @Value("${email.create-documents.text}")
    private String createDocsText;
    @Value("${email.send-documents.subject}")
    private String sendDocsSubject;
    @Value("${email.send-documents.text}")
    private String sendDocsText;
    @Value("${email.send-ses.subject}")
    private String sendSesSubject;
    @Value("${email.send-ses.text1}")
    private String sendSesText1;
    @Value("${email.send-ses.text2}")
    private String sendSesText2;
    @Value("${email.credit-issued.subject}")
    private String creditIssuedSubject;
    @Value("${email.credit-issued.text}")
    private String creditIssuedText;
    @Value("${email.statement-denied.subject}")
    private String statementDeniedSubject;
    @Value("${email.statement-denied.text}")
    private String statementDeniedText;
    @Value("${document.title}")
    private String documentTitle;
    @Value("${document.format.number}")
    private String numberFormat;
    @Value("${document.format.date}")
    private String dateFormat;
    @Value("${document.margin.small}")
    private int smallMarginSize;
    @Value("${document.margin.big}")
    private int bigMarginSize;
    @Value("${document.body.details.title}")
    private String detailsTitle;
    @Value("${document.body.details.amount-text}")
    private String amountText;
    @Value("${document.body.details.term-text}")
    private String termText;
    @Value("${document.body.details.payment-text}")
    private String paymentText;
    @Value("${document.body.details.rate-text}")
    private String rateText;
    @Value("${document.body.details.psk-text}")
    private String pskText;
    @Value("${document.body.details.insurance-text}")
    private String insuranceText;
    @Value("${document.body.details.salary-client-text}")
    private String salaryClientText;
    @Value("${document.body.details.yes-text}")
    private String yesText;
    @Value("${document.body.details.no-text}")
    private String noText;
    @Value("${document.body.details.rate-units-text}")
    private String rateUnitsText;
    @Value("${document.body.details.money-units-text}")
    private String moneyUnitsText;
    @Value("${document.body.details.term-units-text}")
    private String termUnitsText;
    @Value("${document.body.schedule.title}")
    private String scheduleTitle;
    @Value("${document.body.schedule.column-text}")
    private String columnText;
    @Value("${document.body.schedule.row-text}")
    private String rowText;
    @Value("${document.body.schedule.number-text}")
    private String numberText;
    @Value("${document.body.schedule.date-text}")
    private String dateText;
    @Value("${document.body.schedule.total-payment-text}")
    private String totalPaymentText;
    @Value("${document.body.schedule.interest-payment-text}")
    private String interestPaymentText;
    @Value("${document.body.schedule.debt-payment-text}")
    private String debtPaymentText;
    @Value("${document.body.schedule.remaining-debt-text}")
    private String remainingDebtText;

    public SimpleMailMessage getFinishRegMessage(EmailMessage messageInfo) {
        return createSimpleMailMessage(messageInfo.getAddress(), finishRegSubject, finishRegText);
    }

    public SimpleMailMessage getDocsMessage(EmailMessage messageInfo) {
        return createSimpleMailMessage(messageInfo.getAddress(), createDocsSubject, createDocsText);
    }

    public MimeMessage getSendDocsMessage(EmailMessage messageInfo) {
        ByteArrayResource document = generateDocument(messageInfo.getStatementId().toString());
        log.debug("Document with credit details for {} generated", messageInfo.getAddress());
        return createMimeMessage(messageInfo.getAddress(), sendDocsSubject, sendDocsText, document);
    }

    public SimpleMailMessage getSendSesMessage(EmailMessage messageInfo) {
        return createSimpleMailMessage(messageInfo.getAddress(), sendSesSubject, sendSesText1 + messageInfo.getText() +
                sendSesText2 + gatewayUri + baseDocumentUrl + messageInfo.getStatementId() + signDocumentUrl + messageInfo.getText());
    }

    public SimpleMailMessage getCreditIssuedMessage(EmailMessage messageInfo) {
        return createSimpleMailMessage(messageInfo.getAddress(), creditIssuedSubject, creditIssuedText);
    }

    public SimpleMailMessage getStatementDeniedMessage(EmailMessage messageInfo) {
        return createSimpleMailMessage(messageInfo.getAddress(), statementDeniedSubject, statementDeniedText + messageInfo.getText());
    }

    private SimpleMailMessage createSimpleMailMessage(String address, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderName);
        message.setTo(address);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    private ByteArrayResource generateDocument(String statementId) {
        CreditDto creditDto = getCreditDto(statementId);
        if (creditDto == null) {
            log.error("Failed to get credit from deal service, creditDto is null");
            throw new DealServiceException("Failed to get credit from deal service");
        }
        log.debug("Credit info from deal service got successfully");

        DecimalFormat df = new DecimalFormat(numberFormat);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat);

        StringBuilder sb = new StringBuilder();
        sb.append(detailsTitle).append(NEW_LINE);;
        sb.append(amountText).append(df.format(creditDto.getAmount())).append(moneyUnitsText).append(NEW_LINE);
        sb.append(termText).append(creditDto.getTerm()).append(termUnitsText).append(NEW_LINE);
        sb.append(paymentText).append(df.format(creditDto.getMonthlyPayment())).append(moneyUnitsText).append(NEW_LINE);
        sb.append(rateText).append(df.format(creditDto.getRate())).append(rateUnitsText).append(NEW_LINE);
        sb.append(pskText).append(df.format(creditDto.getPsk())).append(moneyUnitsText).append(NEW_LINE);
        sb.append(insuranceText).append(creditDto.getIsInsuranceEnabled() ? yesText : noText).append(NEW_LINE);
        sb.append(salaryClientText).append(creditDto.getIsSalaryClient() ? yesText : noText).append(NEW_LINE);

        sb.append(NEW_LINE).append(scheduleTitle).append(NEW_LINE);;
        String header = String.join(columnText,
                addSpaces(numberText, smallMarginSize),
                addSpaces(dateText, bigMarginSize),
                addSpaces(totalPaymentText + moneyUnitsText, bigMarginSize),
                addSpaces(interestPaymentText + moneyUnitsText, bigMarginSize),
                addSpaces(debtPaymentText + moneyUnitsText, bigMarginSize),
                addSpaces(remainingDebtText + moneyUnitsText, bigMarginSize));
        sb.append(header).append(NEW_LINE);
        sb.append(rowText.repeat(header.length())).append(NEW_LINE);

        for (PaymentScheduleElementDto payment : creditDto.getPaymentSchedule()) {
            String row = String.join(columnText,
                    addSpaces(String.valueOf(payment.getNumber()), smallMarginSize),
                    addSpaces(payment.getDate().format(dtf), bigMarginSize),
                    addSpaces(df.format(payment.getTotalPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getInterestPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getDebtPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getRemainingDebt()), bigMarginSize));
            sb.append(row).append(NEW_LINE);
        }

        return new ByteArrayResource(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private CreditDto getCreditDto(String statementId) {
        try {
            ResponseEntity<CreditDto> response = restTemplate.exchange(
                    getCreditUrl + statementId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Failed to get credit from deal service, status code {}", e.getStatusCode());
            throw new DealServiceException("Failed to get credit from deal service");
        } catch (Exception e) {
            log.error("Failed to get credit from deal service due to unexpected error: {}", e.getMessage());
            throw new DealServiceException("Failed to get credit from deal service");
        }
    }

    private String addSpaces(String s, int length) {
        return s + SPACE.repeat(length - s.length());
    }

    private MimeMessage createMimeMessage(String address, String subject, String text, ByteArrayResource attachment) {
        try {
            MimeMessage resultMessage = mailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(resultMessage, true);
            messageHelper.setFrom(senderName);
            messageHelper.setTo(address);
            messageHelper.setSubject(subject);
            messageHelper.setText(text);
            messageHelper.addAttachment(documentTitle, attachment);
            return resultMessage;
        } catch (MessagingException e) {
            log.error("Failed to create email", e);
            throw new EmailMessageException("Failed to create email");
        }
    }

    public void sendEmailMessage(SimpleMailMessage message) {
        mailSender.send(message);
        log.debug("Email with subject {} sent successfully to {}", message.getSubject(), message.getTo());
    }

    public void sendEmailMessage(MimeMessage message) {
        try {
            mailSender.send(message);
            log.debug("Email with subject {} sent successfully to {}", message.getSubject(),
                    Arrays.stream(message.getAllRecipients()).findAny());
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new EmailMessageException("Failed to send email");
        }
    }
}

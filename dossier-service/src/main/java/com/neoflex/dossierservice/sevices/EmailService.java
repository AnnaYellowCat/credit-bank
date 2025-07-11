package com.neoflex.dossierservice.sevices;

import com.neoflex.dossierservice.dto.CreditDto;
import com.neoflex.dossierservice.dto.EmailMessage;
import com.neoflex.dossierservice.dto.PaymentScheduleElementDto;
import com.neoflex.dossierservice.enums.EmailMessageTheme;
import com.neoflex.dossierservice.exceptions.DealServiceException;
import com.neoflex.dossierservice.exceptions.EmailMessageException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

import static com.neoflex.dossierservice.enums.EmailMessageTheme.*;

@Slf4j
@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;

    public EmailService(JavaMailSender mailSender, RestTemplate restTemplate) {
        this.mailSender = mailSender;
        this.restTemplate = restTemplate;
    }

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

    public void sendEmailMessage(EmailMessage messageInfo){
        try{
            MimeMessage emailMessage = mailSender.createMimeMessage();
            emailMessage.setFrom(senderName);
            MimeMessageHelper messageHelper = new MimeMessageHelper(emailMessage, true);
            messageHelper.setTo(messageInfo.getAddress());

            EmailMessageTheme theme = messageInfo.getTheme();
            if (theme.equals(FINISH_REGISTRATION)) {
                emailMessage.setSubject("Завершение оформления кредита");
                emailMessage.setText("Завершите оформление кредита");
            }
            if (theme.equals(CREATE_DOCUMENTS)) {
                emailMessage.setSubject("Оформление документов для кредита");
                emailMessage.setText("Перейти к оформлению документов");
            }
            if (theme.equals(SEND_DOCUMENTS)) {
                ByteArrayResource document = generateDocument(messageInfo.getStatementId().toString());
                log.debug("Document with credit details for {} generated", messageInfo.getAddress());
                emailMessage.setSubject("Формирование документов для кредита завершено");
                emailMessage.setText("Ваши документы прикреплены к этому письму");
                messageHelper.addAttachment(documentTitle, document);
            }
            if (theme.equals(SEND_SES)) {
                emailMessage.setSubject("Подписание документов для кредита");
                emailMessage.setText("Ваш код: " + messageInfo.getText() +
                        ". Ссылка для подписания документов: " +
                        gatewayUri + baseDocumentUrl + messageInfo.getStatementId() +
                        signDocumentUrl + messageInfo.getText());
            }
            if (theme.equals(CREDIT_ISSUED)) {
                emailMessage.setSubject("Оформление кредита завершено");
                emailMessage.setText("Кредит успешно оформлен");
            }
            if (theme.equals(STATEMENT_DENIED)) {
                emailMessage.setSubject("Отказ кредита");
                emailMessage.setText("К сожалению, вам отказано в кредите. Причина: " + messageInfo.getText());
            }
            mailSender.send(emailMessage);
            log.debug("Email with subject {} sent successfully to {}", emailMessage.getSubject(), messageInfo.getAddress());
        } catch (MessagingException e) {
            log.error("Failed to send email", e);
            throw new EmailMessageException("Failed to send email");
        }
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
        sb.append("Детали кредита:\n");
        sb.append("Сумма кредита: ").append(df.format(creditDto.getAmount())).append(" руб.\n");
        sb.append("Срок кредита: ").append(creditDto.getTerm()).append(" мес.\n");
        sb.append("Ежемесячный платеж: ").append(df.format(creditDto.getMonthlyPayment())).append(" руб.\n");
        sb.append("Процентная ставка: ").append(df.format(creditDto.getRate())).append("%\n");
        sb.append("Полная стоимость кредита: ").append(df.format(creditDto.getPsk())).append("%\n");
        sb.append("Страховка: ").append(creditDto.getIsInsuranceEnabled() ? "Да" : "Нет").append("\n");
        sb.append("Зарплатный клиент: ").append(creditDto.getIsSalaryClient() ? "Да" : "Нет").append("\n\n");

        sb.append("График платежей:\n");
        String header = String.join(" | ",
                addSpaces("№", smallMarginSize),
                addSpaces("Дата", bigMarginSize),
                addSpaces("Общий платеж", bigMarginSize),
                addSpaces("Проценты", bigMarginSize),
                addSpaces("Основной долг", bigMarginSize),
                addSpaces("Остаток долга", bigMarginSize));
        sb.append(header).append("\n");
        sb.append("-".repeat(header.length())).append("\n");

        for (PaymentScheduleElementDto payment : creditDto.getPaymentSchedule()) {
            String row = String.join(" | ",
                    addSpaces(String.valueOf(payment.getNumber()), smallMarginSize),
                    addSpaces(payment.getDate().format(dtf), bigMarginSize),
                    addSpaces(df.format(payment.getTotalPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getInterestPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getDebtPayment()), bigMarginSize),
                    addSpaces(df.format(payment.getRemainingDebt()), bigMarginSize));
            sb.append(row).append("\n");
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
        return s + " ".repeat(length - s.length());
    }
}

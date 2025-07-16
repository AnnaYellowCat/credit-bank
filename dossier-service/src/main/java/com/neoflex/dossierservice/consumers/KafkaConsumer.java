package com.neoflex.dossierservice.consumers;

import com.neoflex.dossierservice.dto.EmailMessage;
import com.neoflex.dossierservice.sevices.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {
    private final EmailService emailService;

    @KafkaListener(topics = "finish-registration")
    public void readFinishRegTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: finish-registration, recipient: {}", message.getAddress());
        SimpleMailMessage emailMessage = emailService.getFinishRegMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }

    @KafkaListener(topics = "create-documents")
    public void readCreateDocsTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: create-documents, recipient: {}", message.getAddress());
        SimpleMailMessage emailMessage = emailService.getDocsMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }

    @KafkaListener(topics = "send-documents")
    public void readSendDocsTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: send-documents, recipient: {}", message.getAddress());
        MimeMessage emailMessage = emailService.getSendDocsMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }

    @KafkaListener(topics = "send-ses")
    public void readSendSesTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: send-ses, recipient: {}", message.getAddress());
        SimpleMailMessage emailMessage = emailService.getSendSesMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }

    @KafkaListener(topics = "credit-issued")
    public void readCreditIssuedTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: credit-issued, recipient: {}", message.getAddress());
        SimpleMailMessage emailMessage = emailService.getCreditIssuedMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }

    @KafkaListener(topics = "statement-denied")
    public void readStatementDeniedTopic(EmailMessage message) {
        log.debug("Received message from kafka - topic: statement-denied, recipient: {}", message.getAddress());
        SimpleMailMessage emailMessage = emailService.getStatementDeniedMessage(message);
        emailService.sendEmailMessage(emailMessage);
    }
}

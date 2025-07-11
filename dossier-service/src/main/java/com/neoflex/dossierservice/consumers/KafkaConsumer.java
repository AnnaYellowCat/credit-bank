package com.neoflex.dossierservice.consumers;

import com.neoflex.dossierservice.mappers.MapMapper;
import com.neoflex.dossierservice.sevices.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class KafkaConsumer {
    private final EmailService emailService;
    private final MapMapper mapMapper;

    public KafkaConsumer(EmailService emailService, MapMapper mapMapper) {
        this.emailService = emailService;
        this.mapMapper = mapMapper;
    }

    @KafkaListener(topics = {"finish-registration", "create-documents", "send-documents",
            "send-ses", "credit-issued", "statement-denied"
    })
    public void readMessage(Map<String, Object> messageData) throws MessagingException {
        log.debug("Received message from kafka - theme: {}, recipient: {}",
                messageData.get("theme"), messageData.get("address"));
        emailService.sendEmailMessage(mapMapper.toEmailMessage(messageData));
    }
}

package com.neoflex.dealservice.producers;

import com.neoflex.dealservice.dto.EmailMessage;
import com.neoflex.dealservice.exceptions.KafkaSendingMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, EmailMessage> kafkaTemplate;

    public void sendMessage(EmailMessage message, String topic) {
        try {
            kafkaTemplate.send(topic, message.getStatementId().toString(), message);
            log.debug("Message for statement with id {} sent to kafka topic {}",
                    message.getStatementId(), topic);
        } catch (Exception e) {
            log.error("Failed to send kafka message: {}", e.getMessage());
            throw new KafkaSendingMessageException("Failed to send kafka message");
        }
    }
}

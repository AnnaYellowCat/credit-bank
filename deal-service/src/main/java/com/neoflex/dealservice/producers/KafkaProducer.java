package com.neoflex.dealservice.producers;

import com.neoflex.dealservice.dto.EmailMessage;
import com.neoflex.dealservice.exceptions.KafkaSendingMessageException;
import com.neoflex.dealservice.mappers.EmailMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class KafkaProducer {
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;
    private final EmailMessageMapper emailMessageMapper;

    public KafkaProducer(KafkaTemplate<String, Map<String, Object>> kafkaTemplate, EmailMessageMapper emailMessageMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.emailMessageMapper = emailMessageMapper;
    }

    public void sendMessage(EmailMessage message) {
        try {
            SendResult<String, Map<String, Object>> result = kafkaTemplate.send(message.getTheme()
                            .toString()
                            .toLowerCase()
                            .replace("_", "-"),
                    message.getStatementId().toString(),
                    emailMessageMapper.toMap(message)).get();
            log.debug("Message for statement with id {} sent to kafka topic {}",
                    message.getStatementId(), result.getRecordMetadata().topic());
        } catch (InterruptedException e) {
            log.error("Interrupted while sending message", e);
            throw new KafkaSendingMessageException("Interrupted while sending message: " + e.getMessage());
        } catch (ExecutionException e) {
            log.error("Failed to send message", e.getCause());
            throw new KafkaSendingMessageException("Failed to send message: " + e.getCause().getMessage());
        }
    }
}

package com.neoflex.dealservice.exceptions;

public class KafkaSendingMessageException extends RuntimeException {
    public KafkaSendingMessageException(String message) {
        super(message);
    }
}
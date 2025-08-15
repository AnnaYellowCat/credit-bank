package com.neoflex.dealservice.exceptions;

public class DocumentIsAlreadySignedException extends RuntimeException {
    public DocumentIsAlreadySignedException(String message) {
        super(message);
    }
}

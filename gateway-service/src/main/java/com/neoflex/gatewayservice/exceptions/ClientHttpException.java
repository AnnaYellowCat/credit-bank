package com.neoflex.gatewayservice.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public class ClientHttpException extends RuntimeException {
    private final HttpStatusCode statusCode;
}
package com.aitip.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ReceiptOcrUnavailableException extends RuntimeException {
    public ReceiptOcrUnavailableException(String message) {
        super(message);
    }
}

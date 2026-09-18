package com.sunmoon.backend.exception.customize;

import org.springframework.http.HttpStatus;

public class ConflictException extends CommonException {
    public ConflictException(String message) {
        super(message);
        this.setHttpStatus(HttpStatus.CONFLICT);
    }
}

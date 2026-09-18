package com.sunmoon.backend.exception.customize;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CommonException {
    public NotFoundException(String message) {
        super(message);
        this.setHttpStatus(HttpStatus.NOT_FOUND);
    }
}

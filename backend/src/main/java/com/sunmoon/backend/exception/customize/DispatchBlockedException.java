package com.sunmoon.backend.exception.customize;

import lombok.Getter;
import java.util.Map;

@Getter
public class DispatchBlockedException extends RuntimeException {
    private final Map<String, Object> preCheckDetails;

    public DispatchBlockedException(String message, Map<String, Object> preCheckDetails) {
        super(message);
        this.preCheckDetails = preCheckDetails;
    }
}

package com.chriniko.fc.statistics.error;

public class BusinessProcessingException extends RuntimeException {

    public BusinessProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessProcessingException(String message) {
        super(message);
    }
}

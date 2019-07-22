package com.chriniko.fc.statistics.error;

public class ProcessingException extends RuntimeException {

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessingException(String message) {
        super(message);
    }
}

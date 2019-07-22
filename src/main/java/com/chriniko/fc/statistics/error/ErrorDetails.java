package com.chriniko.fc.statistics.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorDetails {

    private final Date timestamp;
    private final String message;
    private final String details;

    private final List<ValidationError> validationErrors;

    public ErrorDetails(Date timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.validationErrors = new ArrayList<>();
    }
}

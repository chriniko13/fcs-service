package com.chriniko.fc.statistics.error;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ValidationError {

    private final String objectPath;
    private final String errorMessage;

}

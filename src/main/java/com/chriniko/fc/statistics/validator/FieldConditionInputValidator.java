package com.chriniko.fc.statistics.validator;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.error.BusinessValidationException;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
public class FieldConditionInputValidator {

    public void validate(FieldConditionCapture input) {
        @NotNull Double vegetation = input.getVegetation();
        if (vegetation < 0) {
            throw new BusinessValidationException("provided vegetation should not be negative.");
        }
    }

}

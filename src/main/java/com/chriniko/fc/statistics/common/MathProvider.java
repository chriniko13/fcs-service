package com.chriniko.fc.statistics.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MathProvider {

    public double scale(double input, int scale) {
        return BigDecimal
                .valueOf(input)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }


}

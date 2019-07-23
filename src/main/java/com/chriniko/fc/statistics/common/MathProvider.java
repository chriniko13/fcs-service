package com.chriniko.fc.statistics.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MathProvider {

    public double scale(double v, int scale) {
        return BigDecimal
                .valueOf(v)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

}

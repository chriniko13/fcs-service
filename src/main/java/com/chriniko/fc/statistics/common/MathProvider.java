package com.chriniko.fc.statistics.common;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MathProvider {

    public double getAvg(int noOfRecords, double sum) {
        return BigDecimal
                .valueOf(sum)
                .divide(BigDecimal.valueOf(noOfRecords), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public double scale(double input, int scale) {
        return BigDecimal
                .valueOf(input)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }


}

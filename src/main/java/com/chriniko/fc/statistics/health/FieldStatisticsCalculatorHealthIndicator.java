package com.chriniko.fc.statistics.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component
public class FieldStatisticsCalculatorHealthIndicator extends AbstractHealthIndicator {

    private final FieldStatisticsCalculatorHealthContext calculatorHealthContext;

    @Autowired
    public FieldStatisticsCalculatorHealthIndicator(FieldStatisticsCalculatorHealthContext calculatorHealthContext) {
        this.calculatorHealthContext = calculatorHealthContext;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {

        FieldStatisticsCalculatorHealthStats healthStats = calculatorHealthContext.calculatorHealthStats();

        if (healthStats.getError() == null) {
            builder.up().withDetail("calculator-is-healthy", "YES");
        } else {
            builder.down()
                    .withDetail("calculator-is-healthy", "NO")
                    .withDetail("time-occurred-error", healthStats.getTime().toString())
                    .withException(healthStats.getError());
        }

    }
}

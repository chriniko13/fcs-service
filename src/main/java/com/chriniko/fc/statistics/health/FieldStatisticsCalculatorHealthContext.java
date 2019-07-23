package com.chriniko.fc.statistics.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FieldStatisticsCalculatorHealthContext {

    private final AtomicReference<FieldStatisticsCalculatorHealthStats> statsRef;
    private final Clock clock;

    @Autowired
    public FieldStatisticsCalculatorHealthContext(Clock clock) {
        this.clock = clock;
        this.statsRef = new AtomicReference<>(new FieldStatisticsCalculatorHealthStats());
    }

    public void setError(Exception error) {
        statsRef.updateAndGet(stats -> new FieldStatisticsCalculatorHealthStats(error, Instant.now(clock)));
    }

    FieldStatisticsCalculatorHealthStats calculatorHealthStats() {
        return statsRef.get();
    }

}

package com.chriniko.fc.statistics.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class FieldStatisticsCalculatorHealthContext {

    private final AtomicReference<FieldStatisticsCalculatorHealthStats> statsRef;
    private final Clock utcClock;

    @Autowired
    public FieldStatisticsCalculatorHealthContext(Clock utcClock) {
        this.utcClock = utcClock;
        this.statsRef = new AtomicReference<>(new FieldStatisticsCalculatorHealthStats());
    }

    public void setError(Exception error) {
        statsRef.updateAndGet(stats -> new FieldStatisticsCalculatorHealthStats(error, Instant.now(utcClock)));
    }

    FieldStatisticsCalculatorHealthStats calculatorHealthStats() {
        return statsRef.get();
    }

}

package com.chriniko.fc.statistics.health;

import lombok.Getter;

import java.time.Instant;

@Getter
class FieldStatisticsCalculatorHealthStats {

    private final Exception error;
    private final Instant time;

    FieldStatisticsCalculatorHealthStats() {
        this.error = null;
        this.time = null;
    }

    FieldStatisticsCalculatorHealthStats(Exception error, Instant time) {
        this.error = error;
        this.time = time;
    }
}

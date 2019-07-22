package com.chriniko.fc.statistics.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Data
@NoArgsConstructor

@Measurement(name = "field_condition_capture")
public class FieldConditionCapturePoint {

    @Column(name = "time")
    private Instant occurrenceAt;

    @Column(name = "vegetation")
    private Double vegetation;
}

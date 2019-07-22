package com.chriniko.fc.statistics.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
@Getter
public class MergedFieldConditionCapture {

    private final LocalDate date;
    private final double vegetation;

}

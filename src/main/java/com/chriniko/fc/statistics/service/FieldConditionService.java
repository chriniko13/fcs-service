package com.chriniko.fc.statistics.service;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FieldConditionService {

    private final MeterRegistry meterRegistry;
    private final FieldConditionRepository fieldConditionRepository;

    @Autowired
    public FieldConditionService(FieldConditionRepository fieldConditionRepository, MeterRegistry meterRegistry) {
        this.fieldConditionRepository = fieldConditionRepository;
        this.meterRegistry = meterRegistry;
    }

    public void store(FieldConditionCapture dto) {
        Timer timer = meterRegistry.timer("store");
        timer.record(() -> fieldConditionRepository.save(dto));
    }

    public FieldStatistics getStatistics() {
        Timer timer = meterRegistry.timer("getStatistics");
        return timer.record(() -> new FieldStatistics(fieldConditionRepository.vegetationStatistics()));
    }

}

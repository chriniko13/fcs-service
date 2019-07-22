package com.chriniko.fc.statistics.service;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FieldConditionService {

    private final Map<String, FieldConditionRepository> fieldConditionRepositories;

    @Value("${field-statistics.repository-strategy}")
    private String repositoryStrategy;

    @Autowired
    public FieldConditionService(Map<String, FieldConditionRepository> fieldConditionRepositories) {
        this.fieldConditionRepositories = fieldConditionRepositories;
    }

    public void store(FieldConditionCapture dto) {
        FieldConditionRepository repository = fieldConditionRepositories.get(repositoryStrategy);
        repository.save(dto);
    }

    public FieldStatistics getStatistics() {
        FieldConditionRepository repository = fieldConditionRepositories.get(repositoryStrategy);
        return new FieldStatistics(repository.vegetationStatistics());
    }

}

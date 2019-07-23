package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;

import java.util.List;

public interface FieldConditionRepository {

    void save(FieldConditionCapture capture);

    List<FieldConditionCapture> findAll();

    int noOfRecords();

    VegetationStatistic cachedVegetationStatistics();

    VegetationStatistic vegetationStatistics(int pastDays);

    void updateVegetationStatistics(VegetationStatistic statistic);

    void clear();
}

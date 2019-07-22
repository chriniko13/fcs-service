package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;

import java.util.List;

public interface FieldConditionRepository {

    void save(FieldConditionCapture capture);

    List<FieldConditionCapture> findAll();

    List<MergedFieldConditionCapture> findAllMerged();

    VegetationStatistic vegetationStatistics();

    void updateVegetationStatistics(VegetationStatistic statistic);

    void clear();
}

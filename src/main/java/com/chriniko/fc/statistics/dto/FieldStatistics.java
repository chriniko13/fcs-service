package com.chriniko.fc.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FieldStatistics {

    private VegetationStatistic vegetation = new VegetationStatistic();

}

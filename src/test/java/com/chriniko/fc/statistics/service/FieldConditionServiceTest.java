package com.chriniko.fc.statistics.service;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.noop.NoopTimer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;


@RunWith(MockitoJUnitRunner.class)
public class FieldConditionServiceTest {

    private FieldConditionService fieldConditionService;

    @Mock
    private FieldConditionRepository mockedFieldConditionRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Before
    public void setUp() {
        fieldConditionService = new FieldConditionService(mockedFieldConditionRepository, meterRegistry);
    }

    @Test
    public void store() {

        // given
        FieldConditionCapture capture = new FieldConditionCapture();

        Mockito.when(meterRegistry.timer("store"))
                .thenReturn(
                        new NoopTimer(
                                new Meter.Id("store", null, null, null, Meter.Type.TIMER)
                        )
                );

        // when
        fieldConditionService.store(capture);

        // then
        Mockito.verify(mockedFieldConditionRepository).save(capture);

    }

    @Test
    public void getStatistics() {

        // given
        VegetationStatistic vegetation = new VegetationStatistic(0.32, 0.56, 0.98);


        Mockito.when(meterRegistry.timer("getStatistics"))
                .thenReturn(
                        new NoopTimer(
                                new Meter.Id("getStatistics", null, null, null, Meter.Type.TIMER)
                        )
                );


        Mockito.when(mockedFieldConditionRepository.vegetationStatistics())
                .thenReturn(vegetation);

        // when
        FieldStatistics statistics = fieldConditionService.getStatistics();

        // then
        assertEquals(vegetation, statistics.getVegetation());
    }
}
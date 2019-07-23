package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import org.joor.Reflect;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldConditionRepositoryInMemoryImplTest {

    private static final double DELTA = 1e-15;
    private static final int DAY_IN_SECONDS = 86400;

    private static ExecutorService executorService;

    private FieldConditionRepository fieldConditionRepository;

    @BeforeClass
    public static void globalSetup() {
        executorService = Executors.newFixedThreadPool(20);
    }

    @AfterClass
    public static void globalCleanup() {
        executorService.shutdown();
    }

    @Before
    public void setUp() {
        fieldConditionRepository = new FieldConditionRepositoryInMemoryImpl(
                new MathProvider(),
                Clock.systemUTC(),
                executorService,
                true);
    }

    @Test
    public void clear() {

        // given
        FieldConditionCapture capture = new FieldConditionCapture(0.32, Instant.now());
        fieldConditionRepository.save(capture);
        assertEquals(1, fieldConditionRepository.findAll().size());

        // when
        fieldConditionRepository.clear();

        // then
        assertEquals(0, fieldConditionRepository.findAll().size());
    }

    @Test
    public void save() {

        // given
        FieldConditionCapture capture = new FieldConditionCapture(0.32, Instant.now());

        // when
        fieldConditionRepository.save(capture);

        // then
        assertEquals(1, fieldConditionRepository.findAll().size());
    }

    @Test
    public void findAll() {

        // given
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, Instant.now());
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, Instant.now().plusSeconds(60));
        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, Instant.now().plusSeconds(2 * 60));


        // when
        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);
        fieldConditionRepository.save(capture3);

        // then
        assertEquals(3, fieldConditionRepository.findAll().size());

    }

    @Test
    public void findAllMergedOrderByOccurrenceDesc_single_thread_calculation_approach() {

        // given
        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.minusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.minusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.minusSeconds(2 * DAY_IN_SECONDS));


        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);


        // when
        List<MergedFieldConditionCapture> merged = fieldConditionRepository.findAllMergedOrderByOccurrenceDesc(30);


        // then
        assertEquals(3, merged.size());

        MergedFieldConditionCapture mergedFieldConditionCapture3 = merged.get(0);
        assertEquals(
                (capture1.getVegetation() + capture2.getVegetation()) / 2,
                mergedFieldConditionCapture3.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture2 = merged.get(1);
        assertEquals(
                (capture3.getVegetation() + capture4.getVegetation()) / 2,
                mergedFieldConditionCapture2.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture1 = merged.get(2);
        assertEquals(
                capture5.getVegetation(),
                mergedFieldConditionCapture1.getVegetation(),
                DELTA
        );

    }

    @Test
    public void findAllMergedOrderByOccurrenceDesc_multi_thread_calculation_approach() {

        // given
        Reflect.on(fieldConditionRepository).set("mergedCapturesCalcSingleThreadApproach", false);

        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.minusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.minusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.minusSeconds(2 * DAY_IN_SECONDS));


        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);


        // when
        List<MergedFieldConditionCapture> merged = fieldConditionRepository.findAllMergedOrderByOccurrenceDesc(30);


        // then
        assertEquals(3, merged.size());

        MergedFieldConditionCapture mergedFieldConditionCapture3 = merged.get(0);
        assertEquals(
                (capture1.getVegetation() + capture2.getVegetation()) / 2,
                mergedFieldConditionCapture3.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture2 = merged.get(1);
        assertEquals(
                (capture3.getVegetation() + capture4.getVegetation()) / 2,
                mergedFieldConditionCapture2.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture1 = merged.get(2);
        assertEquals(
                capture5.getVegetation(),
                mergedFieldConditionCapture1.getVegetation(),
                DELTA
        );


        // clean up
        Reflect.on(fieldConditionRepository).set("mergedCapturesCalcSingleThreadApproach", true);
    }


    @Test
    public void findAllMergedOrderByOccurrenceDesc_noOfDaysParam_case_one() {

        // given
        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.minusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.minusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.minusSeconds(2 * DAY_IN_SECONDS));


        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);


        // when
        List<MergedFieldConditionCapture> merged = fieldConditionRepository.findAllMergedOrderByOccurrenceDesc(2);


        // then
        assertEquals(3, merged.size());

        MergedFieldConditionCapture mergedFieldConditionCapture1 = merged.get(0);
        assertEquals(
                (capture1.getVegetation() + capture2.getVegetation()) / 2,
                mergedFieldConditionCapture1.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture2 = merged.get(1);
        assertEquals(
                (capture3.getVegetation() + capture4.getVegetation()) / 2,
                mergedFieldConditionCapture2.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture3 = merged.get(2);
        assertEquals(
                capture5.getVegetation(),
                mergedFieldConditionCapture3.getVegetation(),
                DELTA
        );
    }

    @Test
    public void findAllMergedOrderByOccurrenceDesc_noOfDaysParam_case_two() {

        // given
        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.minusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.minusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.minusSeconds(2 * DAY_IN_SECONDS));


        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);


        // when
        List<MergedFieldConditionCapture> merged = fieldConditionRepository.findAllMergedOrderByOccurrenceDesc(3);


        // then
        assertEquals(3, merged.size());

        MergedFieldConditionCapture mergedFieldConditionCapture3 = merged.get(0);
        assertEquals(
                (capture1.getVegetation() + capture2.getVegetation()) / 2,
                mergedFieldConditionCapture3.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture2 = merged.get(1);
        assertEquals(
                (capture3.getVegetation() + capture4.getVegetation()) / 2,
                mergedFieldConditionCapture2.getVegetation(),
                DELTA
        );

        MergedFieldConditionCapture mergedFieldConditionCapture1 = merged.get(2);
        assertEquals(
                capture5.getVegetation(),
                mergedFieldConditionCapture1.getVegetation(),
                DELTA
        );

    }

    @Test
    public void noOfRecords() {

        // given
        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.plusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.plusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.plusSeconds(2 * DAY_IN_SECONDS));

        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);

        // when
        int records = fieldConditionRepository.noOfRecords();

        // then
        assertEquals(5, records);
    }

    @Test
    public void noOfMergedRecords() {
        // given
        Instant now = Instant.now();
        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now);

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.plusSeconds(DAY_IN_SECONDS));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.plusSeconds(DAY_IN_SECONDS));

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.plusSeconds(2 * DAY_IN_SECONDS));

        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);

        // when
        int records = fieldConditionRepository.noOfMergedRecords();

        // then
        assertEquals(3, records);
    }

    @Test
    public void vegetationStatistics() {

        // given
        VegetationStatistic initValue = new VegetationStatistic();

        // when
        VegetationStatistic vegetationStatistic = fieldConditionRepository.vegetationStatistics();

        // then
        assertEquals(initValue, vegetationStatistic);
    }

    @Test
    public void updateVegetationStatistics() {

        // given
        VegetationStatistic vegetationStatistic = new VegetationStatistic(0.12, 0.87, 0.34);


        // when
        fieldConditionRepository.updateVegetationStatistics(vegetationStatistic);

        // then
        VegetationStatistic result = fieldConditionRepository.vegetationStatistics();
        assertNotNull(vegetationStatistic);
        assertEquals(vegetationStatistic, result);
    }

}
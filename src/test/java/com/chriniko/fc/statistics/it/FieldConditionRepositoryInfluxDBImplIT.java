package com.chriniko.fc.statistics.it;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import com.chriniko.fc.statistics.repository.FieldConditionRepositoryInfluxDBImpl;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldConditionRepositoryInfluxDBImplIT {

    private static final double DELTA = 1e-15;
    private static final int DAY_IN_SECONDS = 86400;


    private FieldConditionRepository fieldConditionRepository;

    private MathProvider mathProvider;

    @Before
    public void setUp() {
        mathProvider = new MathProvider();

        fieldConditionRepository = new FieldConditionRepositoryInfluxDBImpl(
                mathProvider, Clock.systemUTC(), "http://localhost:8086", "user", "123"
        );

        fieldConditionRepository.clear();
    }

    @Test
    public void clear() {

        // given
        FieldConditionCapture capture = new FieldConditionCapture(0.32, Instant.now());
        fieldConditionRepository.save(capture);

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, fieldConditionRepository.findAll().size());
        });

        // when
        fieldConditionRepository.clear();

        // then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(0, fieldConditionRepository.findAll().size());
        });
    }

    @Test
    public void save() {

        // given
        FieldConditionCapture capture = new FieldConditionCapture(0.32, Instant.now());

        // when
        fieldConditionRepository.save(capture);

        // then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(1, fieldConditionRepository.findAll().size());
        });
    }

    @Test
    public void findAll() {

        // given
        Instant now = Instant.now();

        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now);
        FieldConditionCapture capture2 = new FieldConditionCapture(0.33, now);
        FieldConditionCapture capture3 = new FieldConditionCapture(0.34, Instant.now().plusSeconds(60));
        FieldConditionCapture capture4 = new FieldConditionCapture(0.36, Instant.now().plusSeconds(2 * 60));


        // when
        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);
        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        // then
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(4, fieldConditionRepository.findAll().size());
        });

    }

    @Test
    public void cachedVegetationStatistics() {

        // given
        VegetationStatistic initValue = new VegetationStatistic();

        // when
        VegetationStatistic vegetationStatistic = fieldConditionRepository.cachedVegetationStatistics();

        // then
        assertEquals(initValue, vegetationStatistic);
    }

    @Test
    public void vegetationStatistics() {

        // given
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

        FieldConditionCapture capture1 = new FieldConditionCapture(0.32, now.toInstant());
        FieldConditionCapture capture2 = new FieldConditionCapture(0.34, now.minusDays(5).toInstant());

        FieldConditionCapture capture3 = new FieldConditionCapture(0.36, now.minusDays(10).toInstant());
        FieldConditionCapture capture4 = new FieldConditionCapture(0.82, now.minusDays(10).toInstant());

        FieldConditionCapture capture5 = new FieldConditionCapture(0.71, now.minusDays(22).toInstant());

        FieldConditionCapture capture6 = new FieldConditionCapture(0.78, now.minusDays(31).toInstant());

        DoubleSummaryStatistics capturesSummaryStatistics = Stream
                .of(
                        capture1, capture2, capture3, capture4, capture5 /* Note: capture 6 is not in past 30 days*/
                )
                .mapToDouble(FieldConditionCapture::getVegetation)
                .summaryStatistics();


        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);
        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);
        fieldConditionRepository.save(capture5);
        fieldConditionRepository.save(capture6);


        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {

            // when
            VegetationStatistic statistic = fieldConditionRepository.vegetationStatistics(30);


            // then
            assertNotNull(statistic);

            double expectedMax = mathProvider.scale(capturesSummaryStatistics.getMax(), 2);
            assertEquals(expectedMax, statistic.getMax(), DELTA);

            double expectedMin = mathProvider.scale(capturesSummaryStatistics.getMin(), 2);
            assertEquals(expectedMin, statistic.getMin(), DELTA);

            double expectedAvg = mathProvider.scale(capturesSummaryStatistics.getAverage(), 2);
            assertEquals(expectedAvg, statistic.getAvg(), DELTA);
        });
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

        FieldConditionCapture capture6 = new FieldConditionCapture(0.78, now.plusSeconds(4 * DAY_IN_SECONDS));

        fieldConditionRepository.save(capture1);
        fieldConditionRepository.save(capture2);

        fieldConditionRepository.save(capture3);
        fieldConditionRepository.save(capture4);

        fieldConditionRepository.save(capture5);

        fieldConditionRepository.save(capture6);


        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // when - then
            assertEquals(6, fieldConditionRepository.noOfRecords());
        });
    }

    @Test
    public void updateVegetationStatistics() {

        // given
        VegetationStatistic vegetationStatistic = new VegetationStatistic(0.12, 0.87, 0.34);


        // when
        fieldConditionRepository.updateVegetationStatistics(vegetationStatistic);

        // then
        VegetationStatistic result = fieldConditionRepository.cachedVegetationStatistics();
        assertNotNull(vegetationStatistic);
        assertEquals(vegetationStatistic, result);
    }

}
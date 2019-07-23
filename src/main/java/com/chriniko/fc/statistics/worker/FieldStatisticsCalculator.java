package com.chriniko.fc.statistics.worker;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.health.FieldStatisticsCalculatorHealthContext;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2

@Component
public class FieldStatisticsCalculator {

    private final MathProvider mathProvider;

    private final FieldConditionRepository fieldConditionRepository;

    private final PoolHandler poolHandler;

    private final FieldStatisticsCalculatorHealthContext statisticsCalculatorHealthContext;

    @Value("${field-statistics.worker.initial-delay-ms}")
    private long initialDelay;

    /*
        Note: the consistency gap of the field statistics is defined by fixed delay.
     */
    @Value("${field-statistics.worker.fixed-delay-ms}")
    private long fixedDelay;

    @Value("${field-statistics.past-days}")
    private int pastDays;

    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public FieldStatisticsCalculator(FieldConditionRepository fieldConditionRepository,
                                     PoolHandler poolHandler,
                                     MathProvider mathProvider,
                                     FieldStatisticsCalculatorHealthContext statisticsCalculatorHealthContext) {
        this.fieldConditionRepository = fieldConditionRepository;
        this.poolHandler = poolHandler;
        this.mathProvider = mathProvider;
        this.statisticsCalculatorHealthContext = statisticsCalculatorHealthContext;
    }

    @PostConstruct
    void init() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("field-conditions-statistics-calculator");
            return t;
        });

        scheduledExecutorService.scheduleWithFixedDelay(
                this::calculateFieldConditionStatisticsScheduledTask,
                initialDelay,
                fixedDelay,
                TimeUnit.MILLISECONDS
        );

        Runtime.getRuntime().addShutdownHook(new Thread(this::clearResources));
    }

    @PreDestroy
    void clear() {
        clearResources();
    }

    private void calculateFieldConditionStatisticsScheduledTask() {
        try {
            List<MergedFieldConditionCapture> mergedCaptures = fieldConditionRepository.findAllMergedOrderByOccurrenceDesc(pastDays);
            if (mergedCaptures.isEmpty()) {
                return;
            }

            VegetationStatistic freshCalculation = extractStatistic(mergedCaptures);
            fieldConditionRepository.updateVegetationStatistics(freshCalculation);

            // Note: housekeeping operation (if we have captures for more than 200days then clear)
            if (fieldConditionRepository.noOfMergedRecords() > 200) {
                fieldConditionRepository.clear();
            }

        } catch (Exception e) {
            log.error("critical error occurred during calculation of field statistics, message: " + e.getMessage(), e);

            /*
                Note: we should notify this error because is a scheduled job,
                      also from Javadoc of ScheduledExecutorService: `If any execution of the task encounters an exception, subsequent executions are suppressed.`
                      so this is the reason why we catch error(s).

             */
            statisticsCalculatorHealthContext.setError(e);
        }
    }

    private VegetationStatistic extractStatistic(List<MergedFieldConditionCapture> captures) {

        DoubleSummaryStatistics statistics
                = captures.stream().mapToDouble(MergedFieldConditionCapture::getVegetation).summaryStatistics();

        double avg = mathProvider.scale(statistics.getAverage(), 2);

        return new VegetationStatistic(statistics.getMin(), statistics.getMax(), avg);
    }

    private void clearResources() {
        poolHandler.shutdownAndAwaitTermination(scheduledExecutorService);
    }

}

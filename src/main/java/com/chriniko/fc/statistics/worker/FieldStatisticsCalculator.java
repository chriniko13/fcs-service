package com.chriniko.fc.statistics.worker;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2

@Component
public class FieldStatisticsCalculator {

    private final MathProvider mathProvider;

    private final Map<String, FieldConditionRepository> fieldConditionRepositories;

    private final PoolHandler poolHandler;

    @Value("${field-statistics.worker.initial-delay-ms}")
    private long initialDelay;

    /*
        Note: the consistency gap of the field statistics is defined by fixed delay.
     */
    @Value("${field-statistics.worker.fixed-delay-ms}")
    private long fixedDelay;

    @Value("${field-statistics.past-days}")
    private int pastDays;

    @Value("${field-statistics.repository-strategy}")
    private String repositoryStrategy;

    private ScheduledExecutorService scheduledExecutorService;

    @Autowired
    public FieldStatisticsCalculator(Map<String, FieldConditionRepository> fieldConditionRepositories,
                                     PoolHandler poolHandler,
                                     MathProvider mathProvider) {
        this.fieldConditionRepositories = fieldConditionRepositories;
        this.poolHandler = poolHandler;
        this.mathProvider = mathProvider;
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
            FieldConditionRepository fieldConditionRepository = fieldConditionRepositories.get(repositoryStrategy);

            List<MergedFieldConditionCapture> mergedCaptures = fieldConditionRepository.findAllMerged();
            if (mergedCaptures.isEmpty()) {
                return;
            }

            VegetationStatistic freshCalculation = calculateVegetationStatistic(mergedCaptures);
            fieldConditionRepository.updateVegetationStatistics(freshCalculation);

        } catch (Exception e) {
            log.error("critical error occurred during calculation of field statistics, message: " + e.getMessage(), e);

            //TODO health check...
        }
    }

    private VegetationStatistic calculateVegetationStatistic(List<MergedFieldConditionCapture> mergedCaptures) {

        mergedCaptures.sort(Comparator.comparing(MergedFieldConditionCapture::getDate).reversed());

        log.trace("mergedCaptures.size() = {}", mergedCaptures.size());

        List<MergedFieldConditionCapture> lastDaysMergedCaptures
                = mergedCaptures.size() < pastDays
                ? mergedCaptures
                : mergedCaptures.subList(0, pastDays);

        log.trace("lastDaysMergedCaptures.size() = {}", lastDaysMergedCaptures.size());

        return extractStatistic(lastDaysMergedCaptures);
    }

    private VegetationStatistic extractStatistic(List<MergedFieldConditionCapture> captures) {

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0.0D;

        for (MergedFieldConditionCapture capture : captures) {
            @NotNull Double vegetation = capture.getVegetation();
            if (vegetation < min) {
                min = vegetation;
            }
            if (vegetation > max) {
                max = vegetation;
            }
            sum += vegetation;
        }

        double avg = mathProvider.getAvg(captures.size(), sum);

        return new VegetationStatistic(min, max, avg);
    }

    private void clearResources() {
        poolHandler.shutdownAndAwaitTermination(scheduledExecutorService);
    }

}

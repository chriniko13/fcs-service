package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/*
    Note: this repository and worker implementation embraces/uses weakly consistent iterators.
 */

@Log4j2

@Repository(value = "memoRepo")
public class FieldConditionRepositoryInMemoryImpl implements FieldConditionRepository {

    private final MathProvider mathProvider;
    private final Map<LocalDate, Deque<FieldConditionCapture>> capturesGroupByDate;

    private final ZoneId utcZone;

    /*
        Note: Need for atomicity (atomic actions) is for multiple writers, in our case we have one writer (which is scheduler/statistics calculator worker)
              so only visibility from other readers (threads) is our concern
              (this we can solve it by using the `volatile` keyword or the `AtomicReference` structure but the last one is used for more advanced things such as compareAndSet, etc),
              so no need to use atomicity (synchronized, locks, read-write locks, stamped locks, etc)
     */
    private volatile VegetationStatistic vegetationStatistic = new VegetationStatistic();


    @Autowired
    public FieldConditionRepositoryInMemoryImpl(MathProvider mathProvider) {
        capturesGroupByDate = new ConcurrentHashMap<>();
        utcZone = ZoneId.of("UTC");
        this.mathProvider = mathProvider;
    }

    @Override
    public void clear() {
        capturesGroupByDate.clear();
    }

    @Override
    public void save(FieldConditionCapture capture) {

        @NotNull Instant key = capture.getOccurrenceAt();

        LocalDate localDate = key.atZone(utcZone).toLocalDate();

        capturesGroupByDate.computeIfPresent(localDate, (instant, fieldConditionCaptures) -> {
            fieldConditionCaptures.add(capture);
            return fieldConditionCaptures;
        });

        capturesGroupByDate.computeIfAbsent(localDate, instant -> {
            ConcurrentLinkedDeque<FieldConditionCapture> captures = new ConcurrentLinkedDeque<>();
            captures.add(capture);
            return captures;
        });

    }

    @Override
    public List<FieldConditionCapture> findAll() {
        return capturesGroupByDate
                .values()
                .stream()
                .flatMap(Deque::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<MergedFieldConditionCapture> findAllMerged() {
        return calculateMergedCaptures(capturesGroupByDate);
    }

    @Override
    public VegetationStatistic vegetationStatistics() {
        return vegetationStatistic;
    }

    @Override
    public void updateVegetationStatistics(VegetationStatistic statistic) {
        vegetationStatistic.setMin(statistic.getMin());
        vegetationStatistic.setMax(statistic.getMax());
        vegetationStatistic.setAvg(statistic.getAvg());
    }


    //TODO extract to property...
    private static boolean SINGLE_THREAD_APPROACH = true;

    private List<MergedFieldConditionCapture> calculateMergedCaptures(Map<LocalDate, Deque<FieldConditionCapture>> capturesGroupByDate) {
        if (capturesGroupByDate.isEmpty()) {
            return Collections.emptyList();
        }

        if (SINGLE_THREAD_APPROACH) {

            final List<MergedFieldConditionCapture> mergedCaptures = new LinkedList<>();

            for (Map.Entry<LocalDate, Deque<FieldConditionCapture>> entry : capturesGroupByDate.entrySet()) {

                LocalDate date = entry.getKey();
                Deque<FieldConditionCapture> captures = entry.getValue();
                int noOfRecords = captures.size();

                double sum = 0.0D;
                for (FieldConditionCapture capture : captures) {
                    sum += capture.getVegetation();
                }
                double avg = mathProvider.getAvg(noOfRecords, sum);

                MergedFieldConditionCapture merged = new MergedFieldConditionCapture(date, avg);
                mergedCaptures.add(merged);
            }

            return mergedCaptures;

        } else {

            throw new UnsupportedOperationException("TODO");

        }
    }

}

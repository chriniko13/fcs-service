package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.error.BusinessProcessingException;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/*
    Note: this repository and worker implementation embraces/uses weakly consistent iterators
          (we sacrifice a little bit consistency for having scalability, otherwise we will need to use read/write locks during operations which
          take place on field: `ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> capturesGroupByDate`).
 */

@Log4j2

@Repository(value = "memoRepo")
public class FieldConditionRepositoryInMemoryImpl implements FieldConditionRepository {

    private final MathProvider mathProvider;
    private final ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> capturesGroupByDate;

    private final Clock clock;
    private final ExecutorService computationWorkers;

    @Value("${memoRepo.merged-captures.single-thread-approach}")
    private final boolean mergedCapturesCalcSingleThreadApproach;

    /*
        Note: Need for atomicity (atomic actions) is for multiple writers, in our case we have one writer (which is scheduler/statistics calculator worker)
              so only visibility from other readers (threads) is our concern
              (this we can solve it by using the `volatile` keyword or the `AtomicReference` structure but the last one is used for more advanced things such as compareAndSet, etc),
              so no need to use atomicity (synchronized, locks, read-write locks, stamped locks, etc)
     */
    private volatile VegetationStatistic vegetationStatistic = new VegetationStatistic();


    @Autowired
    public FieldConditionRepositoryInMemoryImpl(MathProvider mathProvider,
                                                Clock clock,
                                                @Qualifier("computation-workers") ExecutorService computationWorkers,
                                                @Value("${memoRepo.merged-captures.single-thread-approach}") boolean mergedCapturesCalcSingleThreadApproach) {
        this.clock = clock;
        this.mergedCapturesCalcSingleThreadApproach = mergedCapturesCalcSingleThreadApproach;
        this.capturesGroupByDate = new ConcurrentHashMap<>();
        this.mathProvider = mathProvider;
        this.computationWorkers = computationWorkers;
    }

    @Override
    public void clear() {
        capturesGroupByDate.clear();
    }

    @Override
    public void save(FieldConditionCapture capture) {

        @NotNull Instant occurrenceAt = capture.getOccurrenceAt();
        LocalDate localDate = occurrenceAt.atZone(clock.getZone()).toLocalDate();

        capturesGroupByDate.compute(localDate, (_occurrenceAt, _captures) -> {
            if (_captures == null) {
                _captures = new ConcurrentLinkedDeque<>();
                _captures.add(capture);
                return _captures;
            } else {
                _captures.add(capture);
                return _captures;
            }
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
    public VegetationStatistic vegetationStatistics() {
        return vegetationStatistic;
    }

    @Override
    public void updateVegetationStatistics(VegetationStatistic statistic) {
        vegetationStatistic = statistic;
    }

    @Override
    public List<MergedFieldConditionCapture> findAllMergedOrderByOccurrenceDesc(int pastDays) {
        if (capturesGroupByDate.isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.nanoTime();

        final List<MergedFieldConditionCapture> mergedFieldConditionCaptures;

        MergedCapturesCalculationStrategy calculationStrategy;
        if (mergedCapturesCalcSingleThreadApproach) {
            calculationStrategy = new MergedCapturesCalculationSingleThreadStrategy(pastDays);
        } else {
            calculationStrategy = new MergedCapturesCalculationMultiThreadStrategy(pastDays);
        }
        mergedFieldConditionCaptures = calculationStrategy.calculateMergedCaptures();

        List<MergedFieldConditionCapture> result = mergedFieldConditionCaptures
                .stream()
                .sorted(Comparator.comparing(MergedFieldConditionCapture::getDate).reversed())
                .filter(fC -> isInLastDays(fC.getDate(), pastDays))
                .collect(Collectors.toList());

        long totalTime = System.nanoTime() - startTime;
        log.trace("total time took to calculate findAllMergedOrderByOccurrenceDesc---multithread: "
                + !mergedCapturesCalcSingleThreadApproach
                + ", in ms: "
                + TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS)
        );

        return result;
    }

    @Override
    public int noOfMergedRecords() {
        return capturesGroupByDate.size();
    }

    @Override
    public int noOfRecords() {
        return capturesGroupByDate.entrySet().stream().mapToInt(c -> c.getValue().size()).sum();
    }

    private boolean isInLastDays(LocalDate date, int pastDays) {
        LocalDate nowLocalDate = LocalDate.now(clock);

        long daysDiff = Duration
                .between(
                        date.atStartOfDay(),
                        nowLocalDate.atStartOfDay()
                ).toDays();

        return daysDiff >= 0 && daysDiff <= pastDays;
    }

    // ------ internals ------

    abstract class MergedCapturesCalculationStrategy {
        protected final int pastDays;

        protected MergedCapturesCalculationStrategy(int pastDays) {
            this.pastDays = pastDays;
        }

        protected abstract List<MergedFieldConditionCapture> calculateMergedCaptures();

        MergedFieldConditionCapture calculateMergedCapture(LocalDate date, ConcurrentLinkedDeque<FieldConditionCapture> captures) {

            double avg = captures.stream().mapToDouble(FieldConditionCapture::getVegetation).average().orElse(0.0D);
            avg = mathProvider.scale(avg, 2);

            return new MergedFieldConditionCapture(date, avg);
        }

        protected ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> keepRecordsInLastDays(
                ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> input,
                int pastDays) {
            return input
                    .entrySet()
                    .stream()
                    .filter(entry -> isInLastDays(entry.getKey(), pastDays))
                    .collect(
                            Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (captures1, captures2) -> {
                                        captures1.addAll(captures2);
                                        return captures1;
                                    },
                                    ConcurrentHashMap::new
                            )
                    );
        }
    }

    final class MergedCapturesCalculationSingleThreadStrategy extends MergedCapturesCalculationStrategy {

        MergedCapturesCalculationSingleThreadStrategy(int pastDays) {
            super(pastDays);
        }

        @Override
        public List<MergedFieldConditionCapture> calculateMergedCaptures() {
            final LinkedList<MergedFieldConditionCapture> mergedCaptures = new LinkedList<>();

            keepRecordsInLastDays(capturesGroupByDate, pastDays).forEach((date, captures) -> {
                MergedFieldConditionCapture merged = calculateMergedCapture(date, captures);
                mergedCaptures.add(merged);
            });

            return mergedCaptures;
        }
    }

    final class MergedCapturesCalculationMultiThreadStrategy extends MergedCapturesCalculationStrategy {

        private static final long WAIT_TIME_FOR_CALC_IN_MS = 200;
        private static final int PARTITION_SIZE = 10;

        MergedCapturesCalculationMultiThreadStrategy(int pastDays) {
            super(pastDays);
        }

        @Override
        public List<MergedFieldConditionCapture> calculateMergedCaptures() {
            final ConcurrentLinkedDeque<MergedFieldConditionCapture> mergedCaptures = new ConcurrentLinkedDeque<>();

            ConcurrentHashMap<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>> filteredEntries = keepRecordsInLastDays(capturesGroupByDate, pastDays);

            List<Map.Entry<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>>> capturesToProcess = new ArrayList<>(filteredEntries.entrySet());

            List<List<Map.Entry<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>>>> capturesToProcessPerWorker = Lists.partition(capturesToProcess, PARTITION_SIZE);

            CountDownLatch calculationFinished = new CountDownLatch(capturesToProcessPerWorker.size());

            for (List<Map.Entry<LocalDate, ConcurrentLinkedDeque<FieldConditionCapture>>> capturesByDate : capturesToProcessPerWorker) {

                Runnable workerTask = () -> {
                    capturesByDate.forEach(entry -> {
                        MergedFieldConditionCapture merged = calculateMergedCapture(entry.getKey(), entry.getValue());
                        mergedCaptures.add(merged);
                    });

                    calculationFinished.countDown();
                };

                computationWorkers.submit(workerTask);
            }

            wait(calculationFinished);

            return new LinkedList<>(mergedCaptures);
        }

        private void wait(CountDownLatch calculationFinished) {
            boolean reachedZero = false;
            try {
                reachedZero = calculationFinished.await(WAIT_TIME_FOR_CALC_IN_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw new BusinessProcessingException("processing of merged captures failed", e);
                }
            }
            if (!reachedZero) {
                throw new BusinessProcessingException("processing of merged captures failed");
            }
        }
    }

}

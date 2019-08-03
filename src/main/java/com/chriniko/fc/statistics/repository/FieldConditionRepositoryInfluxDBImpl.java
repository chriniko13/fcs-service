package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.common.MathProvider;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.entity.FieldConditionCapturePoint;
import com.chriniko.fc.statistics.error.BusinessProcessingException;
import lombok.extern.log4j.Log4j2;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Log4j2

@Repository(value = "influxRepo")
public class FieldConditionRepositoryInfluxDBImpl implements FieldConditionRepository {

    private static final String DB_NAME = "field_captures";
    private static final String RETENTION_POLICY = "defaultPolicy";

    // Important Note: watch out for this if tests are failing, because occurrenceAt is very 'old', so are not stored to InfluxDB.
    private static final String RETENTION_POLICY_PERIOD = "300d";

    private final MathProvider mathProvider;
    private final Clock utcClock;

    private final String influxDbUrl;
    private final String influxDbUser;
    private final String influxDbPass;

    private InfluxDB influxDB;
    private InfluxDBResultMapper influxDBResultMapper;

    /*
        Note: Need for atomicity (atomic actions) is for multiple writers, in our case we have one writer (which is scheduler/statistics calculator worker)
              so only visibility from other readers (threads) is our concern
              (this we can solve it by using the `volatile` keyword or the `AtomicReference` structure but the last one is used for more advanced things such as compareAndSet, etc),
              so no need to use atomicity (synchronized, locks, read-write locks, stamped locks, etc)
     */
    private volatile VegetationStatistic vegetationStatistic = new VegetationStatistic();


    @Autowired
    public FieldConditionRepositoryInfluxDBImpl(MathProvider mathProvider,
                                                Clock utcClock,
                                                @Value("${influx-db.url}") String influxDbUrl,
                                                @Value("${influx-db.user}") String influxDbUser,
                                                @Value("${influx-db.pass}") String influxDbPass) {
        this.mathProvider = mathProvider;
        this.utcClock = utcClock;

        this.influxDbUrl = influxDbUrl;
        this.influxDbUser = influxDbUser;
        this.influxDbPass = influxDbPass;
        setupInfluxDb();

        Runtime.getRuntime().addShutdownHook(new Thread(this::clearResources));
    }

    private void setupInfluxDb() {

        this.influxDB = InfluxDBFactory.connect(influxDbUrl, influxDbUser, influxDbPass);
        this.influxDBResultMapper = new InfluxDBResultMapper();

        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            String errorMsg = "Error pinging InfluxDB server.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        } else {
            log.debug("success pinging InfluxDB server, version: {}", response.getVersion());
        }

        influxDB.enableGzip();
        influxDB.setLogLevel(InfluxDB.LogLevel.NONE);

        influxDB.query(new Query("DROP DATABASE " + DB_NAME, DB_NAME));
        influxDB.query(new Query("DROP RETENTION POLICY " + RETENTION_POLICY + " ON " + DB_NAME, DB_NAME));

        influxDB.query(new Query("CREATE DATABASE " + DB_NAME, DB_NAME));
        influxDB.setDatabase(DB_NAME);

        influxDB.query(new Query("CREATE RETENTION POLICY " + RETENTION_POLICY
                + " ON " + DB_NAME
                + " DURATION " + RETENTION_POLICY_PERIOD
                + " REPLICATION 1 SHARD DURATION 30m DEFAULT", DB_NAME));
        influxDB.setRetentionPolicy(RETENTION_POLICY);

        influxDB.enableBatch(
                BatchOptions.DEFAULTS
                        .actions(2000)
                        .flushDuration(100)
                        .jitterDuration(100)
                        .consistency(InfluxDB.ConsistencyLevel.QUORUM)
                        .bufferLimit(3000)
                        .threadFactory(new ThreadFactory() {
                            private final AtomicInteger idGen = new AtomicInteger();

                            @Override
                            public Thread newThread(Runnable r) {
                                Thread t = new Thread(r);
                                t.setName("influxdb-pool-worker-" + idGen.incrementAndGet());
                                return t;
                            }
                        })
        );
    }

    @PreDestroy
    void clearResourcesHook() {
        clearResources();
    }

    private void clearResources() {
        influxDB.close();
    }

    @Override
    public void clear() {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("delete from field_condition_capture")
                .forDatabase(DB_NAME)
                .create();

        QueryResult queryResult = influxDB.query(q);

        if (queryResult.hasError()) {
            throw new BusinessProcessingException(queryResult.getError());
        }
    }

    @Override
    public void save(FieldConditionCapture capture) {

        @NotNull Instant occurrenceAt = capture.getOccurrenceAt();
        @NotNull Double vegetation = capture.getVegetation();

        Point point = Point.measurement("field_condition_capture")
                .time(occurrenceAt.toEpochMilli(), TimeUnit.MILLISECONDS)
                .addField("vegetation", vegetation)
                .tag("sensor_id", UUID.randomUUID().toString()) // Note: in order to not have time only as PK and have upsertions we do not want.
                .build();

        influxDB.write(point);
    }

    @Override
    public List<FieldConditionCapture> findAll() {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("select * from field_condition_capture")
                .forDatabase(DB_NAME)
                .create();

        QueryResult queryResult = influxDB.query(q);
        if (queryResult.hasError()) {
            throw new BusinessProcessingException(queryResult.getError());
        }

        List<FieldConditionCapturePoint> captures = influxDBResultMapper.toPOJO(queryResult, FieldConditionCapturePoint.class);

        return captures
                .stream()
                .map(c -> new FieldConditionCapture(c.getVegetation(), c.getOccurrenceAt()))
                .collect(Collectors.toList());
    }

    @Override
    public int noOfRecords() {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("select count(*) from field_condition_capture")
                .forDatabase(DB_NAME)
                .create();

        QueryResult queryResult = influxDB.query(q);
        if (queryResult.hasError()) {
            throw new BusinessProcessingException(queryResult.getError());
        }

        double c = extractResultFromAggregateFunction(queryResult);
        return (int) c;
    }

    @Override
    public VegetationStatistic cachedVegetationStatistics() {
        return vegetationStatistic;
    }

    @Override
    public VegetationStatistic vegetationStatistics(int pastDays) {
        ZonedDateTime now = ZonedDateTime.now(utcClock);
        ZonedDateTime oneMonthBefore = now.minusDays(pastDays);

        double avg = getAvg(now, oneMonthBefore);
        double max = getMax(now, oneMonthBefore);
        double min = getMin(now, oneMonthBefore);

        min = mathProvider.scale(min, 2);
        max = mathProvider.scale(max, 2);
        avg = mathProvider.scale(avg, 2);
        return new VegetationStatistic(min, max, avg);
    }

    @Override
    public void updateVegetationStatistics(VegetationStatistic statistic) {
        vegetationStatistic = statistic;
    }

    private double getAvg(ZonedDateTime now, ZonedDateTime oneMonthBefore) {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("select mean(vegetation) from field_condition_capture where time >= $start and time <= $finish")
                .forDatabase(DB_NAME)
                .bind("start", oneMonthBefore.toInstant().toString())
                .bind("finish", now.toInstant().toString())
                .create();
        return extractResultFromAggregateFunction(influxDB.query(q));
    }

    private double getMax(ZonedDateTime now, ZonedDateTime oneMonthBefore) {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("select max(vegetation) from field_condition_capture where time >= $start and time <= $finish")
                .forDatabase(DB_NAME)
                .bind("start", oneMonthBefore.toInstant().toString())
                .bind("finish", now.toInstant().toString())
                .create();
        return extractResultFromAggregateFunction(influxDB.query(q));
    }

    private double getMin(ZonedDateTime now, ZonedDateTime oneMonthBefore) {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("select min(vegetation) from field_condition_capture where time >= $start and time <= $finish")
                .forDatabase(DB_NAME)
                .bind("start", oneMonthBefore.toInstant().toString())
                .bind("finish", now.toInstant().toString())
                .create();
        return extractResultFromAggregateFunction(influxDB.query(q));
    }

    private Double extractResultFromAggregateFunction(QueryResult queryResult) {
        if (queryResult.hasError()) {
            throw new BusinessProcessingException(queryResult.getError());
        }

        return Optional.of(queryResult)
                .map(QueryResult::getResults)
                .map(results -> results.get(0))
                .map(QueryResult.Result::getSeries)
                .map(series -> series.get(0))
                .map(QueryResult.Series::getValues)
                .map(listOfLists -> listOfLists.get(0))
                .map(actualValues -> actualValues.get(1))
                .map(_result -> (double) _result)
                .orElse(0D);
    }
}

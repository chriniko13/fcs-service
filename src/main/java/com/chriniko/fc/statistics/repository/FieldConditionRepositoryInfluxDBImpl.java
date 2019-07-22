package com.chriniko.fc.statistics.repository;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.MergedFieldConditionCapture;
import com.chriniko.fc.statistics.dto.VegetationStatistic;
import com.chriniko.fc.statistics.entity.FieldConditionCapturePoint;
import com.chriniko.fc.statistics.error.ProcessingException;
import lombok.extern.log4j.Log4j2;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.*;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2

@Repository(value = "influxDbRepo")
public class FieldConditionRepositoryInfluxDBImpl implements FieldConditionRepository {

    private static final String DB_NAME = "field_captures";
    private static final String RETENTION_POLICY = "defaultPolicy";

    /*
        Note: Need for atomicity (atomic actions) is for multiple writers, in our case we have one writer (which is scheduler/statistics calculator worker)
              so only visibility from other readers (threads) is our concern
              (this we can solve it by using the `volatile` keyword or the `AtomicReference` structure but the last one is used for more advanced things such as compareAndSet, etc),
              so no need to use atomicity (synchronized, locks, read-write locks, stamped locks, etc)
     */
    private volatile VegetationStatistic vegetationStatistic = new VegetationStatistic();

    private final InfluxDBResultMapper influxDBResultMapper;
    private final ZoneId utcZone;

    private InfluxDB influxDB;

    @Value("${influx-db.url}")
    private String influxDbUrl;

    @Value("${influx-db.user}")
    private String influxDbUser;

    @Value("${influx-db.pass}")
    private String influxDbPass;

    public FieldConditionRepositoryInfluxDBImpl() {
        this.utcZone = ZoneId.of("UTC");
        this.influxDBResultMapper = new InfluxDBResultMapper();
    }

    @PostConstruct
    void init() {
        setupInfluxDb();
        Runtime.getRuntime().addShutdownHook(new Thread(this::clearResources));
    }

    private void setupInfluxDb() {
        this.influxDB = InfluxDBFactory.connect(influxDbUrl, influxDbUser, influxDbPass);

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

        influxDB.query(new Query("CREATE RETENTION POLICY " + RETENTION_POLICY + " ON " + DB_NAME + " DURATION 100d REPLICATION 1 SHARD DURATION 30m DEFAULT", DB_NAME));
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

    /*
    Expected: 0.49
     got: 0.51
 ; vegetation.max
Expected: 0.6
     got: 0.92
 ; vegetation.min
Expected: 0.29
     got: 0.06
     */

    @Override
    public void save(FieldConditionCapture fieldConditionCapture) {

        @NotNull Instant occurrenceAt = fieldConditionCapture.getOccurrenceAt();
        @NotNull Double vegetation = fieldConditionCapture.getVegetation();

        Point point = Point.measurement("field_condition_capture")
                    .time(occurrenceAt.toEpochMilli(), TimeUnit.MILLISECONDS)
                    .addField("vegetation", vegetation)
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

        List<FieldConditionCapturePoint> captures = influxDBResultMapper.toPOJO(queryResult, FieldConditionCapturePoint.class);

        return captures
                .stream()
                .map(c -> new FieldConditionCapture(c.getVegetation(), c.getOccurrenceAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MergedFieldConditionCapture> findAllMerged() {
        // Note: it is the same with find all just the type changes, this is because, during save operation we merge the records if exist.
        return findAll()
                .stream()
                .map(r -> new MergedFieldConditionCapture(
                                ZonedDateTime.ofInstant(r.getOccurrenceAt(), utcZone).toLocalDate(),
                                r.getVegetation()
                        )
                )
                .collect(Collectors.toList());
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

    @Override
    public void clear() {
        BoundParameterQuery q = BoundParameterQuery.QueryBuilder
                .newQuery("delete from field_condition_capture")
                .forDatabase(DB_NAME)
                .create();

        QueryResult queryResult = influxDB.query(q);

        if (queryResult.hasError()) {
            throw new ProcessingException(queryResult.getError());
        }
    }
}

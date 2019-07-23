package com.chriniko.fc.statistics.it;

import com.chriniko.fc.statistics.Bootstrap;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.it.core.FileSupport;
import com.chriniko.fc.statistics.it.core.Specification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Bootstrap.class,
        properties = {"application.properties"}
)

@RunWith(SpringRunner.class)
public class GetFieldConditionsStatisticsIT extends Specification {

    private static final double DELTA = 1e-15;

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void get_field_conditions_statistics_case() throws Exception {

        // given
        String data = FileSupport.read("request/get_field_conditions_statistics_1.json");
        List<FieldConditionCapture> captures = objectMapper.readValue(data, new TypeReference<List<FieldConditionCapture>>() {
        });

        System.out.println("~~~ total captures to save: " + captures.size() + " ~~~");

        ExecutorService workersPool = Executors.newFixedThreadPool(captures.size());

        List<CompletableFuture<Void>> cfs = captures.stream()
                .map(capture -> {
                    return CompletableFuture.runAsync(() -> {
                        HttpEntity<FieldConditionCapture> httpEntity = createHttpEntity(capture);
                        String url = getBaseUrl(port);

                        try {
                            ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
                            Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                            System.err.println(e);
                        }

                    }, workersPool);
                })
                .collect(Collectors.toList());

        CompletableFuture<Void> allFinished = CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0]));
        allFinished.join();

        System.out.println("~~~ creation of captures from concurrent clients finished ~~~");


        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    // when
                    ResponseEntity<FieldStatistics> responseEntity = restTemplate.exchange(getBaseUrl(port), HttpMethod.GET, null, FieldStatistics.class);

                    // then
                    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

                    FieldStatistics fieldStatistics = responseEntity.getBody();

                    String expectedAsString = FileSupport.read("response/get_field_conditions_statistics_1.json");
                    FieldStatistics expected = objectMapper.readValue(expectedAsString, FieldStatistics.class);

                    Assert.assertEquals(expected.getVegetation().getMin(), fieldStatistics.getVegetation().getMin(), DELTA);
                    Assert.assertEquals(expected.getVegetation().getMax(), fieldStatistics.getVegetation().getMax(), DELTA);
                    Assert.assertEquals(expected.getVegetation().getAvg(), fieldStatistics.getVegetation().getAvg(), DELTA);

                });


        // clear
        workersPool.shutdown();

    }
}

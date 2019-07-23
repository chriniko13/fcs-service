package com.chriniko.fc.statistics.it;


import com.chriniko.fc.statistics.Bootstrap;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.it.core.FileSupport;
import com.chriniko.fc.statistics.it.core.Specification;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import lombok.Getter;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Bootstrap.class,
        properties = {"application.properties"}
)

@RunWith(SpringRunner.class)
public class FieldConditionCaptureWithInfluxRepoIT extends Specification {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FieldConditionRepository fieldConditionRepository;

    @Test
    public void one_capture_case() {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_1.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);

        // when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);


        // then
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());


        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertEquals(1, records.size());
        });

    }

    @Test
    public void two_captures_with_same_occurrence_case() {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_1.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);


        // when
        IntStream.rangeClosed(1, 2)
                .forEach(idx -> {

                    // then
                    ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
                    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

                });

        // and then
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertEquals(2, records.size());
        });

    }

    @Test
    public void multiple_captures_with_same_occurrence_from_concurrent_clients_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        int clients = 150;
        int capturesPerClient = 3;

        // when
        MultipleClientsOperation multipleClientsOperation
                = new MultipleClientsOperation(clients, capturesPerClient, "request/store_field_condition_1.json")
                .invoke();

        boolean reachedZero = multipleClientsOperation.getLatch().await(25, TimeUnit.SECONDS);
        if (!reachedZero) {
            throw new IllegalStateException("multiple clients operation not successful");
        }


        // then
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            int recordsSize = records.size();
            try {
                Assert.assertEquals(clients * capturesPerClient, recordsSize);
                return true;
            } catch (AssertionError e) {
                System.err.println("total records are: " + recordsSize);
                return false;
            }
        });

        // clear
        ForkJoinPool pool = multipleClientsOperation.getPool();
        pool.shutdown();

    }

    // --- utils ---

    @Getter
    private class MultipleClientsOperation {

        private final int clients;
        private final int capturesPerClient;
        private final CountDownLatch latch;
        private final ForkJoinPool pool;
        private final Runnable task;

        MultipleClientsOperation(int clients, int capturesPerClient, String resourceName) {
            this.clients = clients;
            this.capturesPerClient = capturesPerClient;
            this.latch = new CountDownLatch(clients);
            this.pool = new ForkJoinPool();

            this.task = () -> {
                String payload = FileSupport.read(resourceName);

                HttpEntity<String> httpEntity = createHttpEntity(payload);
                String url = getBaseUrl(port);

                for (int i = 1; i <= capturesPerClient; i++) {
                    ResponseEntity<Void> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
                    Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                }

                latch.countDown();
            };

        }

        MultipleClientsOperation invoke() {
            for (int i = 1; i <= clients; i++) {
                pool.submit(task);
            }
            return this;
        }
    }

}

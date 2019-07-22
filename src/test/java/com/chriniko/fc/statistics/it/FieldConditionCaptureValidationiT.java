package com.chriniko.fc.statistics.it;


import com.chriniko.fc.statistics.Bootstrap;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.it.core.FileSupport;
import com.chriniko.fc.statistics.it.core.Specification;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import com.chriniko.fc.statistics.service.FieldConditionService;
import com.chriniko.fc.statistics.worker.FieldStatisticsCalculator;
import org.joor.Reflect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Bootstrap.class,
        properties = {"application.properties"}
)

@RunWith(SpringRunner.class)
public class FieldConditionCaptureValidationiT extends Specification {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Map<String, FieldConditionRepository> fieldConditionRepositories;

    private FieldConditionRepository fieldConditionRepository;

    @Autowired
    private ApplicationContext applicationContext;


    @Before
    public void setup() {
        String repoToUse = "memoRepo";

        fieldConditionRepository = fieldConditionRepositories.get(repoToUse);

        Reflect.on(applicationContext.getBean(FieldStatisticsCalculator.class))
                .set("repositoryStrategy", repoToUse);

        Reflect.on(applicationContext.getBean(FieldConditionService.class))
                .set("repositoryStrategy", repoToUse);
    }

    @Test
    public void capture_does_not_have_vegetation_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_1.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);

        // when
        try {
            restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
            Assert.fail();
        } catch (HttpClientErrorException error) {

            // then
            Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

            String response = error.getResponseBodyAsString();

            JSONAssert.assertEquals(
                    FileSupport.read("response/store_field_condition_invalid_1.json"),
                    response,
                    new CustomComparator(
                            JSONCompareMode.STRICT,
                            new Customization("timestamp", (o1, o2) -> true)
                    )
            );


            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertTrue(records.isEmpty());
        }

    }

    @Test
    public void capture_has_wrong_vegetation_type_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_2.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);

        // when
        try {
            restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
            Assert.fail();
        } catch (HttpClientErrorException error) {

            // then
            Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

            String response = error.getResponseBodyAsString();

            JSONAssert.assertEquals(
                    FileSupport.read("response/store_field_condition_invalid_2.json"),
                    response,
                    new CustomComparator(
                            JSONCompareMode.STRICT,
                            new Customization("timestamp", (o1, o2) -> true)
                    )
            );


            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertTrue(records.isEmpty());
        }

    }

    @Test
    public void capture_has_missing_occurrence_at_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_3.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);

        // when
        try {
            restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
            Assert.fail();
        } catch (HttpClientErrorException error) {

            // then
            Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

            String response = error.getResponseBodyAsString();

            JSONAssert.assertEquals(
                    FileSupport.read("response/store_field_condition_invalid_3.json"),
                    response,
                    new CustomComparator(
                            JSONCompareMode.STRICT,
                            new Customization("timestamp", (o1, o2) -> true)
                    )
            );


            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertTrue(records.isEmpty());
        }
    }

    @Test
    public void capture_has_wrong_occurrence_at_type_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_4.json");

        HttpEntity<String> httpEntity = createHttpEntity(payload);
        String url = getBaseUrl(port);

        // when
        try {
            restTemplate.exchange(url, HttpMethod.POST, httpEntity, Void.class);
            Assert.fail();
        } catch (HttpClientErrorException error) {

            // then
            Assert.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

            String response = error.getResponseBodyAsString();

            JSONAssert.assertEquals(
                    FileSupport.read("response/store_field_condition_invalid_4.json"),
                    response,
                    new CustomComparator(
                            JSONCompareMode.STRICT,
                            new Customization("timestamp", (o1, o2) -> true)
                    )
            );


            List<FieldConditionCapture> records = fieldConditionRepository.findAll();
            Assert.assertTrue(records.isEmpty());
        }

    }

    //TODO negative vegetation case (create validator)

}

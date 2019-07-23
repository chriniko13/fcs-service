package com.chriniko.fc.statistics.it;


import com.chriniko.fc.statistics.Bootstrap;
import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.it.core.FileSupport;
import com.chriniko.fc.statistics.it.core.Specification;
import com.chriniko.fc.statistics.repository.FieldConditionRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = Bootstrap.class,
        properties = {"application.properties"}
)

@RunWith(SpringRunner.class)
public class FieldConditionCaptureValidationIT extends Specification {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FieldConditionRepository fieldConditionRepository;

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

    @Test
    public void capture_has_wrong_occurrence_format_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_5.json");

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
                    FileSupport.read("response/store_field_condition_invalid_5.json"),
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
    public void capture_has_negative_vegetation_case() throws Exception {

        // given
        fieldConditionRepository.clear();

        String payload = FileSupport.read("request/store_field_condition_invalid_6.json");

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
                    FileSupport.read("response/store_field_condition_invalid_6.json"),
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


}

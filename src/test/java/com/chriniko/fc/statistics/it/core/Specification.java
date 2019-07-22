package com.chriniko.fc.statistics.it.core;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public abstract class Specification {

    protected String getBaseUrl(int port) {
        return "http://localhost:" + port + "/field-conditions";
    }

    protected HttpEntity<String> createHttpEntity(String payload) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");

        return new HttpEntity<>(payload, httpHeaders);
    }

    protected HttpEntity<FieldConditionCapture> createHttpEntity(FieldConditionCapture capture) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");

        return new HttpEntity<>(capture, httpHeaders);
    }


}

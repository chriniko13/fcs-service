package com.chriniko.fc.statistics.it.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Profile("integration")
@Configuration
public class ConfigIT {

    public static String TIME_POINT_FOR_TESTING = "2019-07-29T22:00:00Z";

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Clock clock() {
        return Clock.fixed(Instant.parse(TIME_POINT_FOR_TESTING), ZoneOffset.UTC);
    }

}

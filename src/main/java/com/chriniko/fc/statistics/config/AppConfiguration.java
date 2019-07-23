package com.chriniko.fc.statistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;


@Profile("dev")
@Configuration
public class AppConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}

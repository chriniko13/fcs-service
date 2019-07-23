package com.chriniko.fc.statistics.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Profile("dev")
@Configuration
public class AppConfiguration {

    @Bean
    @Qualifier("computation-workers")
    ExecutorService computationWorkers() {
        return new ThreadPoolExecutor(
                20,
                40,
                30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(3000)
        );
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}

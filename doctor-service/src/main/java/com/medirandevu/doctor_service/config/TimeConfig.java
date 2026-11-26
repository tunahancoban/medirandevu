package com.medirandevu.doctor_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    // Single time source for the app; inject this instead of calling now() so tests can use a fixed clock
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Istanbul"));
    }
}
package com.corvian.payroll_payment_orchestrator.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}

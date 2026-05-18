package com.corvian.payroll_payment_orchestrator.shared.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String PAYROLL_EXECUTION_QUEUE = "payroll.execution.queue";

    @Bean
    public Queue payrollExecutionQueue() {
        return new Queue(PAYROLL_EXECUTION_QUEUE, true);
    }
}

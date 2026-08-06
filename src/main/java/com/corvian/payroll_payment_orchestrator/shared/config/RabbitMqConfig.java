package com.corvian.payroll_payment_orchestrator.shared.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {
    public static final String PAYROLL_EXCHANGE = "payroll.events.exchange";
    public static final String PAYROLL_RETRY_EXCHANGE = "payroll.retry.exchange";
    public static final String PAYROLL_DLQ_EXCHANGE = "payroll.dlq.exchange";
    public static final String PAYROLL_EXECUTION_QUEUE = "payroll.execution.queue";
    public static final String PAYROLL_EXECUTION_RETRY_QUEUE = "payroll.execution.retry.queue";
    public static final String PAYROLL_EXECUTION_DLQ = "payroll.execution.dlq";
    public static final String PAYROLL_EXECUTION_ROUTING_KEY = "payroll.execution.requested";

    @Bean public DirectExchange payrollExchange() { return new DirectExchange(PAYROLL_EXCHANGE, true, false); }
    @Bean public DirectExchange payrollRetryExchange() { return new DirectExchange(PAYROLL_RETRY_EXCHANGE, true, false); }
    @Bean public DirectExchange payrollDlqExchange() { return new DirectExchange(PAYROLL_DLQ_EXCHANGE, true, false); }

    @Bean
    public Queue payrollExecutionQueue() {
        return QueueBuilder.durable(PAYROLL_EXECUTION_QUEUE)
                .deadLetterExchange(PAYROLL_RETRY_EXCHANGE)
                .deadLetterRoutingKey(PAYROLL_EXECUTION_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue payrollExecutionRetryQueue() {
        return QueueBuilder.durable(PAYROLL_EXECUTION_RETRY_QUEUE)
                .ttl(30_000)
                .deadLetterExchange(PAYROLL_EXCHANGE)
                .deadLetterRoutingKey(PAYROLL_EXECUTION_ROUTING_KEY)
                .build();
    }

    @Bean public Queue payrollExecutionDlq() { return QueueBuilder.durable(PAYROLL_EXECUTION_DLQ).build(); }

    @Bean
    public Binding payrollExecutionBinding(@Qualifier("payrollExecutionQueue") Queue payrollExecutionQueue, @Qualifier("payrollExchange") DirectExchange payrollExchange) {
        return BindingBuilder.bind(payrollExecutionQueue).to(payrollExchange).with(PAYROLL_EXECUTION_ROUTING_KEY);
    }
    @Bean
    public Binding payrollRetryBinding(@Qualifier("payrollExecutionRetryQueue") Queue payrollExecutionRetryQueue, @Qualifier("payrollRetryExchange") DirectExchange payrollRetryExchange) {
        return BindingBuilder.bind(payrollExecutionRetryQueue).to(payrollRetryExchange).with(PAYROLL_EXECUTION_ROUTING_KEY);
    }
    @Bean
    public Binding payrollDlqBinding(@Qualifier("payrollExecutionDlq") Queue payrollExecutionDlq, @Qualifier("payrollDlqExchange") DirectExchange payrollDlqExchange) {
        return BindingBuilder.bind(payrollExecutionDlq).to(payrollDlqExchange).with(PAYROLL_EXECUTION_ROUTING_KEY);
    }
}

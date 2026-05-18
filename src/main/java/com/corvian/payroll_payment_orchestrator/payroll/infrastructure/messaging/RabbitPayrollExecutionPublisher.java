package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.messaging;

import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.shared.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RabbitPayrollExecutionPublisher implements PayrollExecutionPublisherPort {
    private final RabbitTemplate rabbitTemplate;

    public RabbitPayrollExecutionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishExecutionRequested(UUID batchId) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.PAYROLL_EXECUTION_QUEUE, batchId.toString());
    }
}

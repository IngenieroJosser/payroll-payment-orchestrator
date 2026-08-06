package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.messaging;

import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.shared.config.RabbitMqConfig;
import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.corvian.payroll_payment_orchestrator.shared.messaging.outbox.OutboxService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class RabbitPayrollExecutionPublisher implements PayrollExecutionPublisherPort {
    private final OutboxService outboxService;
    private final RequestMetadataContext metadataContext;
    private final Clock clock;

    public RabbitPayrollExecutionPublisher(OutboxService outboxService, RequestMetadataContext metadataContext, Clock clock) {
        this.outboxService = outboxService;
        this.metadataContext = metadataContext;
        this.clock = clock;
    }

    @Override
    public void publishExecutionRequested(UUID batchId) {
        UUID messageId = UUID.randomUUID();
        PayrollExecutionMessage message = new PayrollExecutionMessage(messageId, 1, batchId,
                OffsetDateTime.now(clock), metadataContext.get().correlationId());
        outboxService.enqueue("PAYROLL_BATCH", batchId, "PAYROLL_EXECUTION_REQUESTED", 1,
                RabbitMqConfig.PAYROLL_EXECUTION_ROUTING_KEY, message);
    }
}

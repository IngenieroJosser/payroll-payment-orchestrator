package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PayrollExecutionMessage(
        UUID messageId,
        int version,
        UUID batchId,
        OffsetDateTime requestedAt,
        String correlationId
) {}

package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PayrollExecutionMessage(UUID batchId, OffsetDateTime requestedAt) {}

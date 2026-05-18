package com.corvian.payroll_payment_orchestrator.audit.presentation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String resourceType,
        UUID resourceId,
        String description,
        OffsetDateTime createdAt
) {}

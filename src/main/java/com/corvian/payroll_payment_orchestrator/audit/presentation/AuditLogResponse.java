package com.corvian.payroll_payment_orchestrator.audit.presentation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String resourceType,
        UUID resourceId,
        String description,
        String actor,
        String actorType,
        UUID tenantId,
        UUID companyId,
        String correlationId,
        String result,
        String oldStatus,
        String newStatus,
        OffsetDateTime createdAt
) {}

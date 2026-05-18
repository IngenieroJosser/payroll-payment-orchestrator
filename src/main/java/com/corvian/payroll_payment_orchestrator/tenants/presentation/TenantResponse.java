package com.corvian.payroll_payment_orchestrator.tenants.presentation;

import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.TenantStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

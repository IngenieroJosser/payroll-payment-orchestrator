package com.corvian.payroll_payment_orchestrator.companies.presentation;

import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        UUID tenantId,
        String legalName,
        String taxId,
        String currency,
        CompanyStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

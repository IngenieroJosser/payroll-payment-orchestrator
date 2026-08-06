package com.corvian.payroll_payment_orchestrator.banks.presentation;

import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BankConnectionResponse(
        UUID id,
        UUID companyId,
        String bankCode,
        String providerKey,
        String environment,
        String baseUrl,
        Integer connectTimeoutMs,
        Integer readTimeoutMs,
        BankConnectionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

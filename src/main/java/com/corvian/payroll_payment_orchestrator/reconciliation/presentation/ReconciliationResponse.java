package com.corvian.payroll_payment_orchestrator.reconciliation.presentation;

import com.corvian.payroll_payment_orchestrator.reconciliation.domain.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReconciliationResponse(UUID id, UUID batchId, String bankReference, String currency,
                                     BigDecimal expectedAmount, BigDecimal bankAmount, BigDecimal differenceAmount,
                                     ReconciliationStatus status, OffsetDateTime createdAt) {}

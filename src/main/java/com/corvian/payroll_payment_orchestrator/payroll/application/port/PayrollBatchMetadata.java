package com.corvian.payroll_payment_orchestrator.payroll.application.port;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PayrollBatchMetadata(
        UUID tenantId,
        UUID companyId,
        String createdBy,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectionReason,
        Long version
) {}

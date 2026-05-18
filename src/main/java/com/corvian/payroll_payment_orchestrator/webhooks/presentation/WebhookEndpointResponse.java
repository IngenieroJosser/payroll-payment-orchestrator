package com.corvian.payroll_payment_orchestrator.webhooks.presentation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        UUID companyId,
        String url,
        Boolean enabled,
        OffsetDateTime createdAt
) {}

package com.corvian.payroll_payment_orchestrator.webhooks.presentation;

import jakarta.validation.constraints.NotBlank;

public record CreateWebhookEndpointRequest(
        @NotBlank String url
) {}

package com.corvian.payroll_payment_orchestrator.webhooks.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWebhookEndpointRequest(@NotBlank @Size(max=500) String url) {}

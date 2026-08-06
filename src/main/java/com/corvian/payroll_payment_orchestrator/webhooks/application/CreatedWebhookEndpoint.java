package com.corvian.payroll_payment_orchestrator.webhooks.application;

import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.WebhookEndpointEntity;

public record CreatedWebhookEndpoint(WebhookEndpointEntity endpoint, String signingSecret) {}

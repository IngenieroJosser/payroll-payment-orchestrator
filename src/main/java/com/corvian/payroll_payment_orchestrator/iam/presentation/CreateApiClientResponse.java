package com.corvian.payroll_payment_orchestrator.iam.presentation;
import java.util.List;
import java.util.UUID;
public record CreateApiClientResponse(UUID id, String clientId, String clientSecret, String name, List<String> scopes) {}

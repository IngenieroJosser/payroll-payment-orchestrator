package com.corvian.payroll_payment_orchestrator.iam.presentation;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
public record CreateApiClientRequest(UUID companyId, @NotBlank String name, List<String> scopes) {}

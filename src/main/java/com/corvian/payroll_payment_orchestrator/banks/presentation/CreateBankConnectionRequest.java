package com.corvian.payroll_payment_orchestrator.banks.presentation;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
public record CreateBankConnectionRequest(UUID companyId, @NotBlank String bankCode, @NotBlank String baseUrl, @NotBlank String apiToken) {}

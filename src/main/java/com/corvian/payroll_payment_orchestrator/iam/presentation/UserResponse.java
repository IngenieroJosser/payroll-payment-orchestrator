package com.corvian.payroll_payment_orchestrator.iam.presentation;
import com.corvian.payroll_payment_orchestrator.iam.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
public record UserResponse(UUID id, UUID tenantId, UUID companyId, String email, String fullName, UserStatus status, List<String> roles, OffsetDateTime createdAt) {}

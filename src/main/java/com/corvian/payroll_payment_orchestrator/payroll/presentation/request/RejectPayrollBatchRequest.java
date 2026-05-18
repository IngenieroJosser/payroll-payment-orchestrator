package com.corvian.payroll_payment_orchestrator.payroll.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPayrollBatchRequest(
        @NotBlank @Size(max = 500) String reason
) {}

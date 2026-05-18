package com.corvian.payroll_payment_orchestrator.payroll.application.command;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePayrollBatchCommand(
        UUID companyId,
        UUID sourceAccountId,
        String currency,
        LocalDate scheduledDate,
        List<CreatePayrollPaymentCommand> payments
) {}

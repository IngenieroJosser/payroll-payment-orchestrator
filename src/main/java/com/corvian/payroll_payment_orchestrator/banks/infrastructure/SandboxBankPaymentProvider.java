package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankPaymentProvider;
import com.corvian.payroll_payment_orchestrator.banks.application.model.*;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Component
public class SandboxBankPaymentProvider implements BankPaymentProvider {
    @Override public String providerKey() { return "SANDBOX"; }

    @Override
    public BankSubmissionResult submitPayrollBatch(BankSubmissionCommand command) {
        return new BankSubmissionResult("SANDBOX-" + command.executionId(), BankSubmissionStatus.ACCEPTED,
                "ACCEPTED", "Sandbox accepted the payroll batch for asynchronous settlement", OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankPaymentStatusResult getBatchStatus(BankStatusQuery query) {
        // Deterministic sandbox: submission is settled when the status endpoint is polled.
        return new BankPaymentStatusResult(query.externalBatchId(), BankSubmissionStatus.SETTLED, "SETTLED",
                java.util.List.of(), OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankPaymentStatusResult getPaymentStatus(BankPaymentStatusQuery query) {
        return new BankPaymentStatusResult(null, BankSubmissionStatus.SETTLED, "SETTLED",
                java.util.List.of(new BankPaymentResult(query.paymentId(), query.externalPaymentId(), "SETTLED",
                        PayrollPaymentStatus.PAID, null, null, OffsetDateTime.now(ZoneOffset.UTC))),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankReconciliationResult reconcile(BankReconciliationCommand command) {
        return new BankReconciliationResult("SANDBOX-RECON-" + command.submissionId(), java.util.List.of(), OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    public BankCapabilities getCapabilities() {
        return new BankCapabilities(true, true, true, true, true, 10_000, Set.of("COP", "USD"));
    }
}

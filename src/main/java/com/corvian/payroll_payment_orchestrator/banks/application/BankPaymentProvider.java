package com.corvian.payroll_payment_orchestrator.banks.application;

import com.corvian.payroll_payment_orchestrator.banks.application.model.*;

public interface BankPaymentProvider {
    String providerKey();
    BankSubmissionResult submitPayrollBatch(BankSubmissionCommand command);
    BankPaymentStatusResult getBatchStatus(BankStatusQuery query);
    BankPaymentStatusResult getPaymentStatus(BankPaymentStatusQuery query);
    BankReconciliationResult reconcile(BankReconciliationCommand command);
    BankCapabilities getCapabilities();
}

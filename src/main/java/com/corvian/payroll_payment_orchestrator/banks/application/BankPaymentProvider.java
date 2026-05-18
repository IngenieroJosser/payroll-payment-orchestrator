package com.corvian.payroll_payment_orchestrator.banks.application;

public interface BankPaymentProvider {
    BankBatchResponse sendPayrollBatch(BankBatchRequest request);
    BankBatchResponse getBatchStatus(String externalBatchId);
}

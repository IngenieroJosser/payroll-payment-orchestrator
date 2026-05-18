package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchRequest;
import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchResponse;
import com.corvian.payroll_payment_orchestrator.banks.application.BankPaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bank.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxBankPaymentProvider implements BankPaymentProvider {
    @Override
    public BankBatchResponse sendPayrollBatch(BankBatchRequest request) {
        return new BankBatchResponse("SANDBOX-" + request.batchId(), "SENT_TO_BANK", "Batch accepted by sandbox bank provider");
    }

    @Override
    public BankBatchResponse getBatchStatus(String externalBatchId) {
        return new BankBatchResponse(externalBatchId, "PAID", "Batch paid by sandbox bank provider");
    }
}

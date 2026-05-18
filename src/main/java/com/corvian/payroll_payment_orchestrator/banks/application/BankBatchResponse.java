package com.corvian.payroll_payment_orchestrator.banks.application;

public record BankBatchResponse(
        String externalBatchId,
        String status,
        String message
) {}

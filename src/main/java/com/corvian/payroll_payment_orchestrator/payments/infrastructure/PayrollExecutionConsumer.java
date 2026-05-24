package com.corvian.payroll_payment_orchestrator.payments.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchRequest;
import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchResponse;
import com.corvian.payroll_payment_orchestrator.banks.application.BankPaymentProvider;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.shared.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PayrollExecutionConsumer {
    private static final Logger log = LoggerFactory.getLogger(PayrollExecutionConsumer.class);

    private final PayrollBatchUseCase payrollBatchUseCase;
    private final BankPaymentProvider bankPaymentProvider;

    public PayrollExecutionConsumer(PayrollBatchUseCase payrollBatchUseCase, BankPaymentProvider bankPaymentProvider) {
        this.payrollBatchUseCase = payrollBatchUseCase;
        this.bankPaymentProvider = bankPaymentProvider;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYROLL_EXECUTION_QUEUE)
    public void consume(String batchIdAsString) {
        UUID batchId = UUID.fromString(batchIdAsString);
        try {
            PayrollBatch batch = payrollBatchUseCase.findById(batchId);
            BankBatchResponse response = bankPaymentProvider.sendPayrollBatch(new BankBatchRequest(
                    batch.id(),
                    batch.companyId(),
                    batch.totalAmount(),
                    batch.totalPayments(),
                    batch.currency()
            ));
            log.info("Payroll batch sent to bank. batchId={}, externalBatchId={}", batchId, response.externalBatchId());
            payrollBatchUseCase.markAsSentToBank(batchId);
            // TODO Phase 2: replace this immediate PAID transition with bank
            // confirmation through polling or signed callbacks before reconciliation.
            payrollBatchUseCase.markAsPaid(batchId);
        } catch (Exception exception) {
            log.error("Payroll execution failed. batchId={}", batchId, exception);
            payrollBatchUseCase.markAsFailed(batchId, exception.getMessage());
        }
    }
}

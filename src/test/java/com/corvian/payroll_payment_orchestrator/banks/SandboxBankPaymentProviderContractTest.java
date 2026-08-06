package com.corvian.payroll_payment_orchestrator.banks;

import com.corvian.payroll_payment_orchestrator.banks.application.model.*;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.SandboxBankPaymentProvider;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxBankPaymentProviderContractTest {
    private final SandboxBankPaymentProvider provider = new SandboxBankPaymentProvider();

    @Test
    void should_accept_an_idempotent_batch_and_settle_it_asynchronously() {
        UUID executionId = UUID.randomUUID();
        BankSubmissionResult submitted = provider.submitPayrollBatch(command(executionId));

        assertThat(submitted.status()).isEqualTo(BankSubmissionStatus.ACCEPTED);
        assertThat(submitted.externalBatchId()).isEqualTo("SANDBOX-" + executionId);

        BankPaymentStatusResult status = provider.getBatchStatus(new BankStatusQuery(
                UUID.randomUUID(), submitted.externalBatchId(), profile(), "corr-1"
        ));

        assertThat(status.status()).isEqualTo(BankSubmissionStatus.SETTLED);
    }

    @Test
    void should_expose_capabilities_before_submission() {
        BankCapabilities capabilities = provider.getCapabilities();

        assertThat(capabilities.batchSubmission()).isTrue();
        assertThat(capabilities.idempotency()).isTrue();
        assertThat(capabilities.maxPaymentsPerBatch()).isGreaterThanOrEqualTo(10_000);
        assertThat(capabilities.supportedCurrencies()).contains("COP");
    }

    @Test
    void should_return_a_normalized_payment_status() {
        UUID paymentId = UUID.randomUUID();
        BankPaymentStatusResult result = provider.getPaymentStatus(new BankPaymentStatusQuery(
                UUID.randomUUID(), paymentId, "EXT-1", profile(), "corr-2"
        ));

        assertThat(result.payments()).singleElement()
                .satisfies(payment -> {
                    assertThat(payment.paymentId()).isEqualTo(paymentId);
                    assertThat(payment.status()).isEqualTo(PayrollPaymentStatus.PAID);
                });
    }

    private static BankSubmissionCommand command(UUID executionId) {
        return new BankSubmissionCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), executionId,
                "PAYROLL-" + executionId, "COP", LocalDate.now().plusDays(1),
                "1234567890",
                List.of(new BankPaymentInstruction(
                        UUID.randomUUID(), "Ana Pérez", "CC", "123456", "001",
                        AccountType.SAVINGS, "0987654321", new BigDecimal("1000.00"), "NOM-1"
                )),
                profile(), "corr-1"
        );
    }

    private static BankConnectionProfile profile() {
        return new BankConnectionProfile(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "SANDBOX",
                "SANDBOX", "TEST", URI.create("https://sandbox.invalid"), null,
                1_000, 2_000
        );
    }
}

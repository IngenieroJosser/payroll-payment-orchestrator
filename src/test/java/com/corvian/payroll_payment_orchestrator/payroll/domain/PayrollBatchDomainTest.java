package com.corvian.payroll_payment_orchestrator.payroll.domain;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.InvalidStateTransitionException;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayrollBatchDomainTest {

    @Test
    void should_enforce_the_complete_approval_flow() {
        PayrollBatch batch = validBatch();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        PayrollBatch approved = batch
                .transitionTo(PayrollBatchStatus.VALIDATING, now.plusSeconds(1))
                .transitionTo(PayrollBatchStatus.VALIDATED, now.plusSeconds(2))
                .transitionTo(PayrollBatchStatus.PENDING_APPROVAL, now.plusSeconds(3))
                .transitionTo(PayrollBatchStatus.APPROVED, now.plusSeconds(4));

        assertThat(approved.status()).isEqualTo(PayrollBatchStatus.APPROVED);
        assertThat(approved.updatedAt()).isEqualTo(now.plusSeconds(4));
    }

    @Test
    void should_reject_skipping_financial_state_transitions() {
        assertThatThrownBy(() -> validBatch().transitionTo(PayrollBatchStatus.PAID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("PAID");
    }

    @Test
    void should_reject_a_total_that_does_not_match_the_payment_snapshot() {
        PayrollPayment payment = payment();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        assertThatThrownBy(() -> new PayrollBatch(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "COP",
                LocalDate.now().plusDays(1), PayrollBatchStatus.DRAFT,
                new BigDecimal("999.00"), 1, List.of(payment), now, now
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("total amount");
    }

    @Test
    void should_normalize_financial_identifiers_and_amount_scale() {
        PayrollPayment payment = new PayrollPayment(
                UUID.randomUUID(), "cc", " 123456 ", " Ana Pérez ", "001",
                AccountType.SAVINGS, "1234-5678 90", new BigDecimal("1000.00"), null
        );

        assertThat(payment.employeeDocumentType()).isEqualTo("CC");
        assertThat(payment.employeeDocumentNumber()).isEqualTo("123456");
        assertThat(payment.accountNumber()).isEqualTo("1234567890");
        assertThat(payment.amount()).isEqualByComparingTo("1000.00");
        assertThat(payment.status()).isEqualTo(PayrollPaymentStatus.PENDING);
    }

    private static PayrollBatch validBatch() {
        PayrollPayment payment = payment();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new PayrollBatch(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "cop",
                LocalDate.now().plusDays(1), PayrollBatchStatus.DRAFT,
                payment.amount(), 1, List.of(payment), now, now
        );
    }

    private static PayrollPayment payment() {
        return new PayrollPayment(
                UUID.randomUUID(), "CC", "123456", "Ana Pérez", "001",
                AccountType.SAVINGS, "1234567890", new BigDecimal("1000.00"),
                PayrollPaymentStatus.PENDING
        );
    }
}

package com.corvian.payroll_payment_orchestrator.payroll.domain.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PayrollBatch(
        UUID id,
        UUID companyId,
        UUID sourceAccountId,
        String currency,
        LocalDate scheduledDate,
        PayrollBatchStatus status,
        BigDecimal totalAmount,
        Integer totalPayments,
        List<PayrollPayment> payments,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    // Definimos el mapa de transiciones válidas para evitar estados ilícitos
    private static final Map<PayrollBatchStatus, EnumSet<PayrollBatchStatus>> VALID_TRANSITIONS = Map.of(
        PayrollBatchStatus.DRAFT, EnumSet.of(PayrollBatchStatus.VALIDATING, PayrollBatchStatus.CANCELLED, PayrollBatchStatus.PENDING_APPROVAL),
        PayrollBatchStatus.VALIDATING, EnumSet.of(PayrollBatchStatus.VALIDATED, PayrollBatchStatus.FAILED),
        PayrollBatchStatus.VALIDATED, EnumSet.of(PayrollBatchStatus.PENDING_APPROVAL, PayrollBatchStatus.CANCELLED),
        PayrollBatchStatus.PENDING_APPROVAL, EnumSet.of(PayrollBatchStatus.APPROVED, PayrollBatchStatus.REJECTED, PayrollBatchStatus.CANCELLED),
        PayrollBatchStatus.APPROVED, EnumSet.of(PayrollBatchStatus.SCHEDULED, PayrollBatchStatus.PROCESSING, PayrollBatchStatus.CANCELLED),
        PayrollBatchStatus.SCHEDULED, EnumSet.of(PayrollBatchStatus.PROCESSING, PayrollBatchStatus.CANCELLED),
        PayrollBatchStatus.PROCESSING, EnumSet.of(PayrollBatchStatus.SENT_TO_BANK, PayrollBatchStatus.PARTIALLY_PAID, PayrollBatchStatus.PAID, PayrollBatchStatus.FAILED),
        PayrollBatchStatus.SENT_TO_BANK, EnumSet.of(PayrollBatchStatus.PAID, PayrollBatchStatus.PARTIALLY_PAID, PayrollBatchStatus.FAILED)
    );

    public PayrollBatch {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto total del lote de nómina debe ser positivo.");
        }
        if (totalPayments == null || totalPayments < 0) {
            throw new IllegalArgumentException("El lote debe contener al menos un pago de nómina.");
        }
        status = status != null ? status : PayrollBatchStatus.DRAFT;
        payments = payments != null ? List.copyOf(payments) : Collections.emptyList();
    }

    public PayrollBatch transitionTo(PayrollBatchStatus targetStatus) {
        if (this.status == targetStatus) {
            return this;
        }
        
        EnumSet<PayrollBatchStatus> allowed = VALID_TRANSITIONS.get(this.status);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new InvalidStateTransitionException(
                "Transición de estado inválida: No se puede cambiar un lote de nómina de " + this.status + " a " + targetStatus
            );
        }

        return new PayrollBatch(
            this.id,
            this.companyId,
            this.sourceAccountId,
            this.currency,
            this.scheduledDate,
            targetStatus,
            this.totalAmount,
            this.totalPayments,
            this.payments,
            this.createdAt,
            OffsetDateTime.now()
        );
    }

    public PayrollBatch copyWithPayments(List<PayrollPayment> newPayments) {
        return new PayrollBatch(
            this.id,
            this.companyId,
            this.sourceAccountId,
            this.currency,
            this.scheduledDate,
            this.status,
            this.totalAmount,
            this.totalPayments,
            newPayments,
            this.createdAt,
            OffsetDateTime.now()
        );
    }
}


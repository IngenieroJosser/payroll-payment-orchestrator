package com.corvian.payroll_payment_orchestrator.payroll.domain.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

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
    private static final Map<PayrollBatchStatus, EnumSet<PayrollBatchStatus>> VALID_TRANSITIONS = Map.ofEntries(
            Map.entry(PayrollBatchStatus.DRAFT, EnumSet.of(PayrollBatchStatus.VALIDATING, PayrollBatchStatus.CANCELLED)),
            Map.entry(PayrollBatchStatus.VALIDATING, EnumSet.of(PayrollBatchStatus.VALIDATED, PayrollBatchStatus.FAILED)),
            Map.entry(PayrollBatchStatus.VALIDATED, EnumSet.of(PayrollBatchStatus.PENDING_APPROVAL, PayrollBatchStatus.CANCELLED)),
            Map.entry(PayrollBatchStatus.PENDING_APPROVAL, EnumSet.of(PayrollBatchStatus.APPROVED, PayrollBatchStatus.REJECTED, PayrollBatchStatus.CANCELLED)),
            Map.entry(PayrollBatchStatus.APPROVED, EnumSet.of(PayrollBatchStatus.SCHEDULED, PayrollBatchStatus.PROCESSING, PayrollBatchStatus.CANCELLED)),
            Map.entry(PayrollBatchStatus.SCHEDULED, EnumSet.of(PayrollBatchStatus.PROCESSING, PayrollBatchStatus.CANCELLED)),
            Map.entry(PayrollBatchStatus.PROCESSING, EnumSet.of(PayrollBatchStatus.SENT_TO_BANK, PayrollBatchStatus.PARTIALLY_PAID, PayrollBatchStatus.PAID, PayrollBatchStatus.FAILED)),
            Map.entry(PayrollBatchStatus.SENT_TO_BANK, EnumSet.of(PayrollBatchStatus.PARTIALLY_PAID, PayrollBatchStatus.PAID, PayrollBatchStatus.FAILED)),
            Map.entry(PayrollBatchStatus.PARTIALLY_PAID, EnumSet.of(PayrollBatchStatus.PAID, PayrollBatchStatus.FAILED))
    );

    public PayrollBatch {
        Objects.requireNonNull(id, "Payroll batch id is required");
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(sourceAccountId, "Source account id is required");
        currency = normalizeCurrency(currency);
        Objects.requireNonNull(scheduledDate, "Scheduled date is required");
        status = status == null ? PayrollBatchStatus.DRAFT : status;
        payments = payments == null ? List.of() : List.copyOf(payments);
        if (payments.isEmpty()) throw new IllegalArgumentException("Payroll batch must contain at least one payment");
        if (totalPayments == null || totalPayments != payments.size()) {
            throw new IllegalArgumentException("Payroll batch payment count is inconsistent");
        }
        BigDecimal calculated = calculateTotal(payments).setScale(2);
        if (totalAmount == null || totalAmount.setScale(2, RoundingMode.UNNECESSARY).compareTo(calculated) != 0) {
            throw new IllegalArgumentException("Payroll batch total amount is inconsistent with its payments");
        }
        totalAmount = calculated;
        Objects.requireNonNull(createdAt, "Created timestamp is required");
        Objects.requireNonNull(updatedAt, "Updated timestamp is required");
    }

    public PayrollBatch transitionTo(PayrollBatchStatus targetStatus) {
        return transitionTo(targetStatus, OffsetDateTime.now());
    }

    public PayrollBatch transitionTo(PayrollBatchStatus targetStatus, OffsetDateTime changedAt) {
        Objects.requireNonNull(targetStatus, "Target status is required");
        if (status == targetStatus) return this;
        EnumSet<PayrollBatchStatus> allowed = VALID_TRANSITIONS.get(status);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new InvalidStateTransitionException("Invalid payroll batch transition from " + status + " to " + targetStatus);
        }
        return new PayrollBatch(id, companyId, sourceAccountId, currency, scheduledDate, targetStatus,
                totalAmount, totalPayments, payments, createdAt, changedAt);
    }

    public PayrollBatch copyWithPayments(List<PayrollPayment> newPayments) {
        return copyWithPayments(newPayments, OffsetDateTime.now());
    }

    public PayrollBatch copyWithPayments(List<PayrollPayment> newPayments, OffsetDateTime changedAt) {
        BigDecimal newTotal = calculateTotal(newPayments);
        return new PayrollBatch(id, companyId, sourceAccountId, currency, scheduledDate, status,
                newTotal, newPayments.size(), newPayments, createdAt, changedAt);
    }

    public boolean isTerminal() {
        return EnumSet.of(PayrollBatchStatus.PAID, PayrollBatchStatus.FAILED, PayrollBatchStatus.CANCELLED, PayrollBatchStatus.REJECTED)
                .contains(status);
    }

    private static BigDecimal calculateTotal(List<PayrollPayment> paymentList) {
        Objects.requireNonNull(paymentList, "Payment list is required");
        BigDecimal total = BigDecimal.ZERO;
        for (PayrollPayment payment : paymentList) {
            total = total.add(Objects.requireNonNull(payment, "Payment is required").amount());
        }
        return total;
    }

    private static String normalizeCurrency(String value) {
        if (value == null || !value.trim().matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO-4217 code");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

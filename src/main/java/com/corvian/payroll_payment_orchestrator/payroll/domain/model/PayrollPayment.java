package com.corvian.payroll_payment_orchestrator.payroll.domain.model;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record PayrollPayment(
        UUID id,
        String employeeDocumentType,
        String employeeDocumentNumber,
        String employeeFullName,
        String bankCode,
        AccountType accountType,
        String accountNumber,
        BigDecimal amount,
        PayrollPaymentStatus status
) {
    public PayrollPayment {
        Objects.requireNonNull(id, "Payment id is required");
        employeeDocumentType = requireText(employeeDocumentType, "Employee document type", 20).toUpperCase(Locale.ROOT);
        employeeDocumentNumber = requireText(employeeDocumentNumber, "Employee document number", 80);
        employeeFullName = requireText(employeeFullName, "Employee full name", 180);
        bankCode = requireText(bankCode, "Destination bank code", 20).toUpperCase(Locale.ROOT);
        Objects.requireNonNull(accountType, "Account type is required");
        accountNumber = normalizeAccount(accountNumber);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        status = status == null ? PayrollPaymentStatus.PENDING : status;
    }

    public PayrollPayment withStatus(PayrollPaymentStatus targetStatus) {
        return new PayrollPayment(id, employeeDocumentType, employeeDocumentNumber, employeeFullName,
                bankCode, accountType, accountNumber, amount, targetStatus);
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return normalized;
    }

    private static String normalizeAccount(String value) {
        String normalized = requireText(value, "Account number", 64).replaceAll("[\\s-]", "");
        if (!normalized.matches("^[A-Za-z0-9]{4,34}$")) {
            throw new IllegalArgumentException("Account number must contain between 4 and 34 alphanumeric characters");
        }
        return normalized;
    }
}

package com.corvian.payroll_payment_orchestrator.shared.util;

public final class MaskingUtils {
    private MaskingUtils() {}

    public static String last4(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - 4);
    }

    public static String maskDocument(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() <= 4) return "*".repeat(Math.max(1, trimmed.length()));
        return "*".repeat(trimmed.length() - 4) + trimmed.substring(trimmed.length() - 4);
    }

    public static String maskAccount(String value) {
        String last4 = last4(value);
        return last4 == null ? null : "****" + last4;
    }
}

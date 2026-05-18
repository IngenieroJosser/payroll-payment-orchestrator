package com.corvian.payroll_payment_orchestrator.shared.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Meta meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Meta.now());
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message), Meta.now());
    }

    public record ApiError(String code, String message) {}
    public record Meta(Instant timestamp) {
        public static Meta now() {
            return new Meta(Instant.now());
        }
    }
}

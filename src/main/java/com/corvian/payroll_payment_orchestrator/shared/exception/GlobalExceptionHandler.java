package com.corvian.payroll_payment_orchestrator.shared.exception;

import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final RequestMetadataContext requestMetadataContext;

    public GlobalExceptionHandler(RequestMetadataContext requestMetadataContext) {
        this.requestMetadataContext = requestMetadataContext;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
        return failure(statusFor(exception.getCode()), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).orElse("Invalid request payload");
        return failure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidInput(Exception exception) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request payload or parameters are invalid");
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(Exception exception) {
        return failure(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The resource changed concurrently; reload it before retrying the operation");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return failure(HttpStatus.CONFLICT, "DATA_INTEGRITY_CONFLICT",
                "The operation conflicts with an existing resource or financial constraint");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        String correlationId = requestMetadataContext.get().correlationId();
        log.error("Unexpected server error. correlationId={}", correlationId, exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Unexpected server error. Use the correlation identifier when contacting support");
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(code, message));
    }

    private HttpStatus statusFor(String code) {
        String normalized = code == null ? "" : code.toUpperCase(Locale.ROOT);
        if (normalized.endsWith("_NOT_FOUND")) return HttpStatus.NOT_FOUND;
        if (normalized.contains("ACCESS_DENIED") || normalized.endsWith("_REQUIRED")
                || normalized.contains("MAKER_CHECKER") || normalized.contains("SCOPE")) return HttpStatus.FORBIDDEN;
        if (normalized.contains("AUTHENTICATION") || normalized.contains("INVALID_CREDENTIAL")
                || normalized.contains("INVALID_CLIENT")) return HttpStatus.UNAUTHORIZED;
        if (normalized.contains("ALREADY_EXISTS") || normalized.contains("REUSED")
                || normalized.contains("IN_PROGRESS") || normalized.contains("CONCURRENT")
                || normalized.contains("INVALID_STATE") || normalized.contains("TRANSITION")
                || normalized.contains("MISMATCH")) return HttpStatus.CONFLICT;
        if (normalized.startsWith("BANK_PROVIDER_HTTP_5") || normalized.contains("UNAVAILABLE")
                || normalized.contains("TIMEOUT")) return HttpStatus.SERVICE_UNAVAILABLE;
        if (normalized.startsWith("BANK_PROVIDER_")) return HttpStatus.BAD_GATEWAY;
        return HttpStatus.BAD_REQUEST;
    }
}

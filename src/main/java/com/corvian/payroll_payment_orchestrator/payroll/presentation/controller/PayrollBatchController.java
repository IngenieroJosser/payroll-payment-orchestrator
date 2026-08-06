package com.corvian.payroll_payment_orchestrator.payroll.presentation.controller;

import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollPaymentCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.payroll.presentation.request.CreatePayrollBatchRequest;
import com.corvian.payroll_payment_orchestrator.payroll.presentation.request.RejectPayrollBatchRequest;
import com.corvian.payroll_payment_orchestrator.payroll.presentation.response.PayrollBatchResponse;
import com.corvian.payroll_payment_orchestrator.payroll.presentation.response.PayrollPaymentResponse;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import com.corvian.payroll_payment_orchestrator.shared.util.MaskingUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll-batches")
public class PayrollBatchController {
    private final CreatePayrollBatchUseCase createPayrollBatchUseCase;
    private final ApprovePayrollBatchUseCase approvePayrollBatchUseCase;
    private final ExecutePayrollBatchUseCase executePayrollBatchUseCase;
    private final PayrollBatchUseCase payrollBatchUseCase;

    public PayrollBatchController(
            CreatePayrollBatchUseCase createPayrollBatchUseCase,
            ApprovePayrollBatchUseCase approvePayrollBatchUseCase,
            ExecutePayrollBatchUseCase executePayrollBatchUseCase,
            PayrollBatchUseCase payrollBatchUseCase
    ) {
        this.createPayrollBatchUseCase = createPayrollBatchUseCase;
        this.approvePayrollBatchUseCase = approvePayrollBatchUseCase;
        this.executePayrollBatchUseCase = executePayrollBatchUseCase;
        this.payrollBatchUseCase = payrollBatchUseCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<ApiResponse<List<PayrollBatchResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(payrollBatchUseCase.findAll().stream().map(this::toResponse).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('payroll:create')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> create(@Valid @RequestBody CreatePayrollBatchRequest request) {
        PayrollBatch createdBatch = createPayrollBatchUseCase.create(toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(createdBatch)));
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> findById(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(payrollBatchUseCase.findById(batchId))));
    }

    @PostMapping("/{batchId}/validate")
    @PreAuthorize("hasAuthority('payroll:create')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> validate(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(payrollBatchUseCase.validate(batchId))));
    }

    @PostMapping("/{batchId}/approve")
    @PreAuthorize("hasAuthority('payroll:approve')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> approve(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(approvePayrollBatchUseCase.approve(batchId))));
    }

    @PostMapping("/{batchId}/reject")
    @PreAuthorize("hasAuthority('payroll:approve')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> reject(
            @PathVariable UUID batchId,
            @Valid @RequestBody RejectPayrollBatchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(approvePayrollBatchUseCase.reject(batchId, request.reason()))));
    }

    @PostMapping("/{batchId}/execute")
    @PreAuthorize("hasAuthority('payroll:execute')")
    public ResponseEntity<ApiResponse<PayrollBatchResponse>> execute(@PathVariable UUID batchId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(toResponse(executePayrollBatchUseCase.execute(batchId))));
    }

    private CreatePayrollBatchCommand toCommand(CreatePayrollBatchRequest request) {
        return new CreatePayrollBatchCommand(
                request.companyId(),
                request.sourceAccountId(),
                request.currency(),
                request.scheduledDate(),
                request.payments().stream()
                        .map(payment -> new CreatePayrollPaymentCommand(
                                payment.employeeDocumentType(),
                                payment.employeeDocumentNumber(),
                                payment.employeeFullName(),
                                payment.bankCode(),
                                payment.accountType(),
                                payment.accountNumber(),
                                payment.amount()
                        ))
                        .toList()
        );
    }

    private PayrollBatchResponse toResponse(PayrollBatch batch) {
        return new PayrollBatchResponse(
                batch.id(),
                batch.companyId(),
                batch.sourceAccountId(),
                batch.currency(),
                batch.scheduledDate(),
                batch.status(),
                batch.totalAmount(),
                batch.totalPayments(),
                batch.payments().stream().map(this::toPaymentResponse).toList(),
                batch.createdAt(),
                batch.updatedAt()
        );
    }

    private PayrollPaymentResponse toPaymentResponse(PayrollPayment payment) {
        return new PayrollPaymentResponse(
                payment.id(),
                payment.employeeDocumentType(),
                MaskingUtils.maskDocument(payment.employeeDocumentNumber()),
                payment.employeeFullName(),
                payment.bankCode(),
                payment.accountType(),
                MaskingUtils.maskAccount(payment.accountNumber()),
                payment.amount(),
                payment.status()
        );
    }
}

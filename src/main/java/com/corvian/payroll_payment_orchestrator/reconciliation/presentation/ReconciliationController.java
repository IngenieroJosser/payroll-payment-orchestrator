package com.corvian.payroll_payment_orchestrator.reconciliation.presentation;

import com.corvian.payroll_payment_orchestrator.reconciliation.application.ReconciliationService;
import com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure.ReconciliationItemEntity;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll-batches/{batchId}/reconciliation")
public class ReconciliationController {
    private final ReconciliationService service;
    public ReconciliationController(ReconciliationService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('reconciliation:manage')")
    public ApiResponse<ReconciliationResponse> reconcile(@PathVariable UUID batchId, @Valid @RequestBody CreateReconciliationRequest request) {
        return ApiResponse.ok(toResponse(service.reconcile(batchId, request.bankReference(), request.bankAmount(), request.sourceEventId(), request.details())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('payroll:read')")
    public ApiResponse<List<ReconciliationResponse>> list(@PathVariable UUID batchId) {
        return ApiResponse.ok(service.findByBatch(batchId).stream().map(this::toResponse).toList());
    }

    private ReconciliationResponse toResponse(ReconciliationItemEntity entity) {
        return new ReconciliationResponse(entity.getId(), entity.getBatchId(), entity.getBankReference(), entity.getCurrency(), entity.getExpectedAmount(), entity.getBankAmount(), entity.getDifferenceAmount(), entity.getStatus(), entity.getCreatedAt());
    }
}

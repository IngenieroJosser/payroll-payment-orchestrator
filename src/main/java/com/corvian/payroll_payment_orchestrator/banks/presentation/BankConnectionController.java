package com.corvian.payroll_payment_orchestrator.banks.presentation;

import com.corvian.payroll_payment_orchestrator.banks.application.BankConnectionService;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.BankConnectionEntity;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bank-connections")
public class BankConnectionController {
    private final BankConnectionService service;
    public BankConnectionController(BankConnectionService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('bank:manage')")
    public ResponseEntity<ApiResponse<BankConnectionResponse>> create(@Valid @RequestBody CreateBankConnectionRequest request) {
        BankConnectionEntity saved = service.create(request.companyId(), request.bankCode(), request.baseUrl(), request.apiToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new BankConnectionResponse(saved.getId(), saved.getCompanyId(), saved.getBankCode(), saved.getBaseUrl(), saved.getStatus(), saved.getCreatedAt())));
    }
}

package com.corvian.payroll_payment_orchestrator.companies.presentation;

import com.corvian.payroll_payment_orchestrator.companies.application.CompanyService;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.BankAccountEntity;
import com.corvian.payroll_payment_orchestrator.companies.infrastructure.CompanyEntity;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
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
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('company:manage')")
    public ResponseEntity<ApiResponse<CompanyResponse>> create(@Valid @RequestBody CreateCompanyRequest request) {
        CompanyEntity created = service.create(request.tenantId(), request.legalName(), request.taxId(), request.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toCompanyResponse(created)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('company:manage') or hasAuthority('payroll:read')")
    public ApiResponse<List<CompanyResponse>> list() {
        return ApiResponse.ok(service.findAll().stream().map(this::toCompanyResponse).toList());
    }

    @GetMapping("/{companyId}")
    @PreAuthorize("hasAuthority('company:manage') or hasAuthority('payroll:read')")
    public ApiResponse<CompanyResponse> findById(@PathVariable UUID companyId) {
        return ApiResponse.ok(toCompanyResponse(service.findById(companyId)));
    }

    @PostMapping("/{companyId}/bank-accounts")
    @PreAuthorize("hasAuthority('company:manage')")
    public ResponseEntity<ApiResponse<BankAccountResponse>> addBankAccount(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateBankAccountRequest request
    ) {
        BankAccountEntity created = service.addBankAccount(companyId, request.bankCode(), request.accountType(), request.accountNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toBankAccountResponse(created)));
    }

    @GetMapping("/{companyId}/bank-accounts")
    @PreAuthorize("hasAuthority('company:manage') or hasAuthority('payroll:read')")
    public ApiResponse<List<BankAccountResponse>> listBankAccounts(@PathVariable UUID companyId) {
        return ApiResponse.ok(service.findBankAccounts(companyId).stream().map(this::toBankAccountResponse).toList());
    }

    private CompanyResponse toCompanyResponse(CompanyEntity entity) {
        return new CompanyResponse(entity.getId(), entity.getTenantId(), entity.getLegalName(), entity.getTaxId(), entity.getCurrency(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private BankAccountResponse toBankAccountResponse(BankAccountEntity entity) {
        return new BankAccountResponse(entity.getId(), entity.getCompanyId(), entity.getBankCode(), entity.getAccountType(), entity.getAccountNumberMasked(), entity.getAccountNumberLast4(), entity.getStatus(), entity.getCreatedAt());
    }
}

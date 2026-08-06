package com.corvian.payroll_payment_orchestrator.tenants.presentation;

import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import com.corvian.payroll_payment_orchestrator.tenants.application.TenantService;
import com.corvian.payroll_payment_orchestrator.tenants.infrastructure.TenantEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tenant:manage')")
    public ResponseEntity<ApiResponse<TenantResponse>> create(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(service.create(request.name(), request.slug()))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:manage')")
    public ApiResponse<List<TenantResponse>> list() {
        return ApiResponse.ok(service.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('tenant:manage')")
    public ApiResponse<TenantResponse> findById(@PathVariable UUID tenantId) {
        return ApiResponse.ok(toResponse(service.findById(tenantId)));
    }

    private TenantResponse toResponse(TenantEntity entity) {
        return new TenantResponse(entity.getId(), entity.getName(), entity.getSlug(), entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}

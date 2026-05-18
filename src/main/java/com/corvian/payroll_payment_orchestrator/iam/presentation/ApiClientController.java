package com.corvian.payroll_payment_orchestrator.iam.presentation;

import com.corvian.payroll_payment_orchestrator.iam.application.IamService;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/iam/api-clients")
public class ApiClientController {
    private final IamService service;
    public ApiClientController(IamService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('iam:manage')")
    public ResponseEntity<ApiResponse<CreateApiClientResponse>> create(@Valid @RequestBody CreateApiClientRequest request) {
        IamService.CreateApiClientResult result = service.createApiClient(request.companyId(), request.name(), request.scopes());
        var entity = result.entity();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(new CreateApiClientResponse(entity.getId(), entity.getClientId(), result.plainSecret(), entity.getName(), request.scopes())));
    }
}

package com.corvian.payroll_payment_orchestrator.iam.presentation;

import com.corvian.payroll_payment_orchestrator.iam.application.AuthorityService;
import com.corvian.payroll_payment_orchestrator.iam.application.IamService;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.UserEntity;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/iam/users")
public class UserController {
    private final IamService service;
    public UserController(IamService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('iam:manage')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserEntity user = service.createUser(request.tenantId(), request.companyId(), request.email(), request.fullName(), request.password(), request.roles());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(user)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('iam:read')")
    public ApiResponse<List<UserResponse>> list() { return ApiResponse.ok(service.findAllUsers().stream().map(this::toResponse).toList()); }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getTenantId(), user.getCompanyId(), user.getEmail(), user.getFullName(), user.getStatus(), AuthorityService.roleNames(user), user.getCreatedAt());
    }
}

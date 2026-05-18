package com.corvian.payroll_payment_orchestrator.audit.presentation;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.AuditLogEntity;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {
    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<List<AuditLogResponse>> list(@RequestParam(required = false) UUID resourceId) {
        List<AuditLogEntity> logs = resourceId == null ? service.findLatest() : service.findByResourceId(resourceId);
        return ApiResponse.ok(logs.stream().map(this::toResponse).toList());
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}

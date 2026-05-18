package com.corvian.payroll_payment_orchestrator.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByResourceIdOrderByCreatedAtDesc(UUID resourceId);
    List<AuditLogEntity> findTop50ByOrderByCreatedAtDesc();
}

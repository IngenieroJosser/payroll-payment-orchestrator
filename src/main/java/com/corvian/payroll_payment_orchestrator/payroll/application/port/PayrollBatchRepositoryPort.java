package com.corvian.payroll_payment_orchestrator.payroll.application.port;

import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollBatchRepositoryPort {
    PayrollBatch save(PayrollBatch payrollBatch);
    Optional<PayrollBatch> findById(UUID id);
    Optional<PayrollBatch> findByIdForUpdate(UUID id);
    List<PayrollBatch> findAll();
    List<PayrollBatch> findByTenantId(UUID tenantId);
    List<PayrollBatch> findByCompanyId(UUID companyId);
    Optional<PayrollBatchMetadata> findMetadata(UUID id);
    void recordApproval(UUID id, String approvedBy, OffsetDateTime approvedAt);
    void recordRejection(UUID id, String rejectionReason);
}

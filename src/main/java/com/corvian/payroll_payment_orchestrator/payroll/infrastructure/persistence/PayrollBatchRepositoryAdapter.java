package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaCompanyRepository;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchMetadata;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.mapper.PayrollBatchMapper;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PayrollBatchRepositoryAdapter implements PayrollBatchRepositoryPort {
    private final JpaPayrollBatchRepository repository;
    private final PayrollBatchMapper mapper;
    private final JpaCompanyRepository companyRepository;
    private final ActorContext actorContext;

    public PayrollBatchRepositoryAdapter(
            JpaPayrollBatchRepository repository,
            PayrollBatchMapper mapper,
            JpaCompanyRepository companyRepository,
            ActorContext actorContext
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.companyRepository = companyRepository;
        this.actorContext = actorContext;
    }

    @Override
    public PayrollBatch save(PayrollBatch payrollBatch) {
        PayrollBatchEntity entity = repository.findById(payrollBatch.id()).orElseGet(() -> {
            UUID tenantId = companyRepository.findById(payrollBatch.companyId())
                    .orElseThrow(() -> new DomainException("COMPANY_NOT_FOUND", "Company was not found"))
                    .getTenantId();
            return mapper.newEntity(payrollBatch, tenantId, actorContext.actorName());
        });
        mapper.updateEntity(payrollBatch, entity);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<PayrollBatch> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PayrollBatch> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<PayrollBatch> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PayrollBatch> findByTenantId(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PayrollBatch> findByCompanyId(UUID companyId) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<PayrollBatchMetadata> findMetadata(UUID id) {
        return repository.findById(id).map(entity -> new PayrollBatchMetadata(entity.getTenantId(), entity.getCompanyId(),
                entity.getCreatedBy(), entity.getApprovedBy(), entity.getApprovedAt(), entity.getRejectionReason(), entity.getVersion()));
    }

    @Override
    public void recordApproval(UUID id, String approvedBy, OffsetDateTime approvedAt) {
        PayrollBatchEntity entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException("PAYROLL_BATCH_NOT_FOUND", "Payroll batch was not found"));
        entity.setApprovedBy(approvedBy);
        entity.setApprovedAt(approvedAt);
        entity.setRejectionReason(null);
        repository.save(entity);
    }

    @Override
    public void recordRejection(UUID id, String rejectionReason) {
        PayrollBatchEntity entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException("PAYROLL_BATCH_NOT_FOUND", "Payroll batch was not found"));
        entity.setRejectionReason(rejectionReason);
        repository.save(entity);
    }
}

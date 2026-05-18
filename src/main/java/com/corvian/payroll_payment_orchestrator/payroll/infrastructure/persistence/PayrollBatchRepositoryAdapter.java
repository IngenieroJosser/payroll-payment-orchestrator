package com.corvian.payroll_payment_orchestrator.payroll.infrastructure.persistence;

import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.mapper.PayrollBatchMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PayrollBatchRepositoryAdapter implements PayrollBatchRepositoryPort {
    private final JpaPayrollBatchRepository repository;
    private final PayrollBatchMapper mapper;

    public PayrollBatchRepositoryAdapter(JpaPayrollBatchRepository repository, PayrollBatchMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public PayrollBatch save(PayrollBatch payrollBatch) {
        PayrollBatchEntity entity = mapper.toEntity(payrollBatch);
        PayrollBatchEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PayrollBatch> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}

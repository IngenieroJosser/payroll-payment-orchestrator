package com.corvian.payroll_payment_orchestrator.iam.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID> { Optional<PermissionEntity> findByName(String name); }

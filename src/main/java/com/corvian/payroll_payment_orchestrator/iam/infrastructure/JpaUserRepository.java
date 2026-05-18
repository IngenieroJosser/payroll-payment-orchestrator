package com.corvian.payroll_payment_orchestrator.iam.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> { Optional<UserEntity> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); }

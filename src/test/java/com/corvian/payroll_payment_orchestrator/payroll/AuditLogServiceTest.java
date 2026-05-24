package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.AuditLogEntity;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.JpaAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private JpaAuditLogRepository repository;

    private AuditLogService service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuditLogService(repository);
    }

    @Test
    @DisplayName("should record audit with minimal context (backward compatibility)")
    void testRecordLegacy() {
        // Arrange
        String action = "PAYROLL_CREATED";
        String resourceType = "PAYROLL_BATCH";
        UUID resourceId = UUID.randomUUID();
        String description = "Payroll batch created successfully";

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        // Act
        service.record(action, resourceType, resourceId, description);

        // Assert
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceType()).isEqualTo(resourceType);
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getDescription()).isEqualTo(description);
        assertThat(saved.getActor()).isEqualTo("SYSTEM");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should record audit with persisted operational context")
    void testRecordWithContext() {
        // Arrange
        String action = "PAYROLL_APPROVED";
        String resourceType = "PAYROLL_BATCH";
        UUID resourceId = UUID.randomUUID();
        String description = "Payroll batch approved";
        String actor = "user@example.com";
        String correlationId = "corr-12345";
        String clientIp = "192.168.1.100";
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        // Act
        service.record(action, resourceType, resourceId, description, actor, correlationId, clientIp, tenantId, companyId);

        // Assert
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceType()).isEqualTo(resourceType);
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getCorrelationId()).isEqualTo(correlationId);
        assertThat(saved.getClientIp()).isEqualTo(clientIp);
    }

    @Test
    @DisplayName("should keep compatibility for state transition overload")
    void testRecordWithStateChange() {
        // Arrange
        String action = "STATUS_CHANGED";
        String resourceType = "PAYROLL_BATCH";
        UUID resourceId = UUID.randomUUID();
        String description = "Status transitioned";
        String actor = "system@corvian.com";
        String correlationId = "corr-67890";
        String clientIp = "10.0.0.1";
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String entityType = "BatchStatus";
        UUID entityId = UUID.randomUUID();
        String oldStatus = "PENDING";
        String newStatus = "APPROVED";
        String result = "SUCCESS";

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        // Act
        service.recordWithStateChange(
            action, resourceType, resourceId, description,
            actor, correlationId, clientIp,
            tenantId, companyId,
            entityType, entityId,
            oldStatus, newStatus,
            result
        );

        // Assert
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getResourceType()).isEqualTo(resourceType);
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getDescription()).isEqualTo(description);
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getCorrelationId()).isEqualTo(correlationId);
        assertThat(saved.getClientIp()).isEqualTo(clientIp);
    }

    @Test
    @DisplayName("should record operation failure with reason")
    void testRecordFailure() {
        // Arrange
        String action = "PAYROLL_EXECUTION_FAILED";
        String resourceType = "PAYROLL_BATCH";
        UUID resourceId = UUID.randomUUID();
        String description = "Payroll execution encountered an error";
        String actor = "system@corvian.com";
        String correlationId = "corr-11111";
        String clientIp = "10.0.0.2";
        String failureReason = "Bank connection timeout after 30 seconds";

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        // Act
        service.recordFailure(
            action, resourceType, resourceId, description,
            actor, correlationId, clientIp,
            failureReason
        );

        // Assert
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(action);
        assertThat(saved.getDescription()).isEqualTo(description);
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getCorrelationId()).isEqualTo(correlationId);
        assertThat(saved.getClientIp()).isEqualTo(clientIp);
    }

    @Test
    @DisplayName("should find audit logs by resource ID")
    void testFindByResourceId() {
        // Arrange
        UUID resourceId = UUID.randomUUID();
        List<AuditLogEntity> expectedLogs = List.of(new AuditLogEntity());
        when(repository.findByResourceIdOrderByCreatedAtDesc(resourceId)).thenReturn(expectedLogs);

        // Act
        List<AuditLogEntity> result = service.findByResourceId(resourceId);

        // Assert
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findByResourceIdOrderByCreatedAtDesc(resourceId);
    }

    @Test
    @DisplayName("should find latest audit logs")
    void testFindLatest() {
        // Arrange
        List<AuditLogEntity> expectedLogs = List.of(new AuditLogEntity());
        when(repository.findTop50ByOrderByCreatedAtDesc()).thenReturn(expectedLogs);

        // Act
        List<AuditLogEntity> result = service.findLatest();

        // Assert
        assertThat(result).isEqualTo(expectedLogs);
        verify(repository).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("should use SYSTEM as default actor if none provided")
    void testDefaultActorIsSYSTEM() {
        // Arrange
        String action = "AUTO_RECONCILIATION";
        String resourceType = "RECONCILIATION";
        UUID resourceId = UUID.randomUUID();
        String description = "Automatic reconciliation check";

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        // Act
        service.record(action, resourceType, resourceId, description, null, null, null, null, null);

        // Assert
        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo("SYSTEM");
    }
}

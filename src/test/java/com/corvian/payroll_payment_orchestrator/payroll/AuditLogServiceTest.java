package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.AuditLogEntity;
import com.corvian.payroll_payment_orchestrator.audit.infrastructure.JpaAuditLogRepository;
import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private JpaAuditLogRepository repository;

    @Mock
    private ActorContext actorContext;

    @Mock
    private RequestMetadataContext requestMetadataContext;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        lenient().when(actorContext.current()).thenReturn(AuthenticatedActor.system());
        lenient().when(requestMetadataContext.get()).thenReturn(new RequestMetadataContext.RequestMetadata(null, null));
        service = new AuditLogService(repository, actorContext, requestMetadataContext);
    }

    @Test
    @DisplayName("should record audit with minimal context")
    void recordsAuditWithMinimalContext() {
        UUID resourceId = UUID.randomUUID();
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        service.record("PAYROLL_CREATED", "PAYROLL_BATCH", resourceId, "Payroll batch created successfully");

        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("PAYROLL_CREATED");
        assertThat(saved.getResourceType()).isEqualTo("PAYROLL_BATCH");
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getDescription()).isEqualTo("Payroll batch created successfully");
        assertThat(saved.getActor()).isEqualTo("SYSTEM");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should record persisted operational context")
    void recordsPersistedOperationalContext() {
        UUID resourceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        service.record("PAYROLL_APPROVED", "PAYROLL_BATCH", resourceId, "Payroll batch approved",
                "user@example.com", "corr-12345", "192.168.1.100", tenantId, companyId);

        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo("user@example.com");
        assertThat(saved.getCorrelationId()).isEqualTo("corr-12345");
        assertThat(saved.getClientIp()).isEqualTo("192.168.1.100");
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getCompanyId()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("should keep compatibility for state transition overload")
    void recordsStateChange() {
        UUID resourceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        service.recordWithStateChange(
                "STATUS_CHANGED", "PAYROLL_BATCH", resourceId, "Status transitioned",
                "system@corvian.com", "corr-67890", "10.0.0.1", tenantId, companyId,
                "BatchStatus", UUID.randomUUID(), "PENDING", "APPROVED", "SUCCESS");

        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getOldStatus()).isEqualTo("PENDING");
        assertThat(saved.getNewStatus()).isEqualTo("APPROVED");
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("should record operation failure with reason")
    void recordsFailure() {
        UUID resourceId = UUID.randomUUID();
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        service.recordFailure("PAYROLL_EXECUTION_FAILED", "PAYROLL_BATCH", resourceId,
                "Payroll execution encountered an error", "system@corvian.com", "corr-11111",
                "10.0.0.2", "Bank connection timeout after 30 seconds");

        verify(repository).save(captor.capture());
        AuditLogEntity saved = captor.getValue();
        assertThat(saved.getResult()).isEqualTo("FAILURE");
        assertThat(saved.getFailureReason()).isEqualTo("Bank connection timeout after 30 seconds");
    }

    @Test
    @DisplayName("should find audit logs by resource ID")
    void findsAuditLogsByResourceId() {
        UUID resourceId = UUID.randomUUID();
        List<AuditLogEntity> expected = List.of(new AuditLogEntity());
        when(repository.findByResourceIdOrderByCreatedAtDesc(resourceId)).thenReturn(expected);

        assertThat(service.findByResourceId(resourceId)).isEqualTo(expected);

        verify(repository).findByResourceIdOrderByCreatedAtDesc(resourceId);
    }

    @Test
    @DisplayName("should find latest audit logs")
    void findsLatestAuditLogs() {
        List<AuditLogEntity> expected = List.of(new AuditLogEntity());
        when(repository.findTop50ByOrderByCreatedAtDesc()).thenReturn(expected);

        assertThat(service.findLatest()).isEqualTo(expected);

        verify(repository).findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("should use SYSTEM as default actor")
    void usesSystemAsDefaultActor() {
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);

        service.record("AUTO_RECONCILIATION", "RECONCILIATION", UUID.randomUUID(),
                "Automatic reconciliation check", null, null, null, null, null);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("SYSTEM");
    }
}

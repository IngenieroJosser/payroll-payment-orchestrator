package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.companies.infrastructure.JpaCompanyRepository;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ResourceAccessService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAccessServiceTest {

    @Test
    void should_reject_cross_tenant_access() {
        UUID actorTenant = UUID.randomUUID();
        ActorContext context = mock(ActorContext.class);
        when(context.current()).thenReturn(new AuthenticatedActor(
                "operator@example.com", ActorType.USER, actorTenant, null, false
        ));
        ResourceAccessService access = new ResourceAccessService(context, mock(JpaCompanyRepository.class));

        assertThatThrownBy(() -> access.requireTenantAccess(UUID.randomUUID()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("authenticated tenant");
    }

    @Test
    void should_allow_platform_administration_across_tenants() {
        ActorContext context = mock(ActorContext.class);
        when(context.current()).thenReturn(new AuthenticatedActor(
                "platform-admin", ActorType.USER, null, null, true
        ));
        ResourceAccessService access = new ResourceAccessService(context, mock(JpaCompanyRepository.class));

        access.requireTenantAccess(UUID.randomUUID());
    }
}

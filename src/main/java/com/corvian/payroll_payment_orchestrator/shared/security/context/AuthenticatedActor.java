package com.corvian.payroll_payment_orchestrator.shared.security.context;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedActor(
        String subject,
        ActorType actorType,
        UUID tenantId,
        UUID companyId,
        boolean platformAdmin
) implements Principal {
    @Override
    public String getName() {
        return subject;
    }

    public static AuthenticatedActor system() {
        return new AuthenticatedActor("SYSTEM", ActorType.SYSTEM, null, null, true);
    }
}

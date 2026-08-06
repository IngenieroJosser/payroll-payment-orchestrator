package com.corvian.payroll_payment_orchestrator.shared.security.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorContext {

    public AuthenticatedActor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return AuthenticatedActor.system();
        }
        if (authentication.getPrincipal() instanceof AuthenticatedActor actor) {
            return actor;
        }
        return new AuthenticatedActor(authentication.getName(), ActorType.USER, null, null, false);
    }

    public String actorName() {
        return current().subject();
    }
}

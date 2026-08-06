package com.corvian.payroll_payment_orchestrator.shared.secrets;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentSecretReferenceResolver implements SecretReferenceResolver {
    private static final String PREFIX = "env:";
    private final Environment environment;

    public EnvironmentSecretReferenceResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String reference) {
        if (reference == null || !reference.startsWith(PREFIX)) {
            throw new DomainException("UNSUPPORTED_SECRET_REFERENCE",
                    "Only env: secret references are supported by this deployment adapter");
        }
        String variable = reference.substring(PREFIX.length());
        if (!variable.matches("^[A-Z][A-Z0-9_]{2,127}$")) {
            throw new DomainException("INVALID_SECRET_REFERENCE", "Secret environment variable name is invalid");
        }
        String value = environment.getProperty(variable);
        if (value == null || value.isBlank()) {
            throw new DomainException("SECRET_REFERENCE_UNRESOLVED",
                    "The configured secret reference is not available to the runtime identity");
        }
        return value;
    }
}

package com.corvian.payroll_payment_orchestrator.banks.resilience;

import com.corvian.payroll_payment_orchestrator.banks.domain.exception.BankProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class BankProviderCircuitBreaker {
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    public BankProviderCircuitBreaker(Clock clock,
            @Value("${app.bank.circuit-breaker-failure-threshold:5}") int failureThreshold,
            @Value("${app.bank.circuit-breaker-open-ms:30000}") long openMs) {
        this.clock = clock;
        this.failureThreshold = Math.max(2, failureThreshold);
        this.openDuration = Duration.ofMillis(Math.max(1000, openMs));
    }

    public <T> T execute(UUID connectionId, Supplier<T> action) {
        State state = states.computeIfAbsent(connectionId, ignored -> new State());
        synchronized (state) {
            Instant now = clock.instant();
            if (state.openUntil != null && now.isBefore(state.openUntil)) {
                throw new BankProviderException("BANK_PROVIDER_CIRCUIT_OPEN", "Bank provider circuit is temporarily open", true);
            }
            if (state.openUntil != null) state.openUntil = null;
        }
        try {
            T result = action.get();
            synchronized (state) { state.failures = 0; state.openUntil = null; }
            return result;
        } catch (BankProviderException ex) {
            if (ex.isRetryable()) recordFailure(state);
            throw ex;
        } catch (RuntimeException ex) {
            recordFailure(state);
            throw ex;
        }
    }

    private void recordFailure(State state) {
        synchronized (state) {
            state.failures++;
            if (state.failures >= failureThreshold) {
                state.openUntil = clock.instant().plus(openDuration);
                state.failures = 0;
            }
        }
    }

    private static final class State { private int failures; private Instant openUntil; }
}

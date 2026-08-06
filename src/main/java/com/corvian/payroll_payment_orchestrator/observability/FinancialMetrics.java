package com.corvian.payroll_payment_orchestrator.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class FinancialMetrics {
    private final MeterRegistry registry;

    public FinancialMetrics(MeterRegistry registry) { this.registry = registry; }

    public void increment(String metric, String... tags) { registry.counter(metric, tags).increment(); }

    public <T> T time(String metric, Callable<T> action, String... tags) {
        Timer.Sample sample = Timer.start(registry);
        try { return action.call(); }
        catch (RuntimeException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalStateException(ex); }
        finally { sample.stop(registry.timer(metric, tags)); }
    }
}

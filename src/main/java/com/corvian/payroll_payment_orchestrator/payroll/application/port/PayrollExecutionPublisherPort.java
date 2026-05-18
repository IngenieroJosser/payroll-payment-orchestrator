package com.corvian.payroll_payment_orchestrator.payroll.application.port;

import java.util.UUID;

public interface PayrollExecutionPublisherPort {
    void publishExecutionRequested(UUID batchId);
}

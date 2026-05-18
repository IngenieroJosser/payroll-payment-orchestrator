package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollPaymentCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollExecutionPublisherPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchService;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PayrollBatchServiceTest {
    @Test
    void shouldCreatePayrollBatchInDraftStatus() {
        PayrollBatchRepositoryPort repository = mock(PayrollBatchRepositoryPort.class);
        PayrollExecutionPublisherPort publisher = mock(PayrollExecutionPublisherPort.class);
        AuditLogService audit = mock(AuditLogService.class);
        WebhookDeliveryService webhook = mock(WebhookDeliveryService.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PayrollBatchService service = new PayrollBatchService(repository, publisher, audit, webhook);

        var command = new CreatePayrollBatchCommand(
                UUID.randomUUID(), UUID.randomUUID(), "COP", LocalDate.now().plusDays(1),
                List.of(new CreatePayrollPaymentCommand("CC", "123", "Juan Perez", "001", AccountType.SAVINGS, "1234567890", new BigDecimal("2500000")))
        );

        PayrollBatch result = service.create(command);

        assertThat(result.status()).isEqualTo(PayrollBatchStatus.DRAFT);
        assertThat(result.totalPayments()).isEqualTo(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("2500000");
        verify(repository).save(any());
    }
}

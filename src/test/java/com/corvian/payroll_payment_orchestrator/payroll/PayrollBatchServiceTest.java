package com.corvian.payroll_payment_orchestrator.payroll;

import com.corvian.payroll_payment_orchestrator.audit.application.AuditLogService;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollBatchCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.command.CreatePayrollPaymentCommand;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.PayrollBatchRepositoryPort;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ApprovePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.CreatePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.port.in.ExecutePayrollBatchUseCase;
import com.corvian.payroll_payment_orchestrator.payroll.application.usecase.PayrollBatchService;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollBatchStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PayrollBatchServiceTest {

    @Test
    void shouldCreatePayrollBatchInDraftStatus() {
        PayrollBatchRepositoryPort repository = mock(PayrollBatchRepositoryPort.class);
        AuditLogService audit = mock(AuditLogService.class);
        WebhookDeliveryService webhook = mock(WebhookDeliveryService.class);

        // Creamos mocks para los casos de uso
        CreatePayrollBatchUseCase createUseCase = mock(CreatePayrollBatchUseCase.class);
        ApprovePayrollBatchUseCase approveUseCase = mock(ApprovePayrollBatchUseCase.class);
        ExecutePayrollBatchUseCase executeUseCase = mock(ExecutePayrollBatchUseCase.class);

        // Mock del repositorio
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock del createUseCase para devolver un PayrollBatch válido
        when(createUseCase.create(any(CreatePayrollBatchCommand.class))).thenAnswer(invocation -> {
            CreatePayrollBatchCommand cmd = invocation.getArgument(0);
            return new PayrollBatch(
                    cmd.companyId(),
                    cmd.sourceAccountId(),
                    cmd.currency(),
                    cmd.scheduledDate(),
                    cmd.payments()
            );
        });

        PayrollBatchService service = new PayrollBatchService(
                repository,
                audit,
                webhook,
                createUseCase,
                approveUseCase,
                executeUseCase
        );

        var command = new CreatePayrollBatchCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "COP",
                LocalDate.now().plusDays(1),
                List.of(new CreatePayrollPaymentCommand(
                        "CC",
                        "123",
                        "Juan Perez",
                        "001",
                        AccountType.SAVINGS,
                        "1234567890",
                        new BigDecimal("2500000")
                ))
        );

        PayrollBatch result = service.create(command);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(PayrollBatchStatus.DRAFT);
        assertThat(result.totalPayments()).isEqualTo(command.payments().size());
        BigDecimal totalAmount = command.payments().stream()
                .map(CreatePayrollPaymentCommand::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result.totalAmount()).isEqualByComparingTo(totalAmount);

        verify(repository).save(any());
    }
}

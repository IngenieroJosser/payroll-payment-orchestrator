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
import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.PayrollPaymentStatus;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollBatch;
import com.corvian.payroll_payment_orchestrator.payroll.domain.model.PayrollPayment;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookDeliveryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

        CreatePayrollBatchUseCase createUseCase = mock(CreatePayrollBatchUseCase.class);
        ApprovePayrollBatchUseCase approveUseCase = mock(ApprovePayrollBatchUseCase.class);
        ExecutePayrollBatchUseCase executeUseCase = mock(ExecutePayrollBatchUseCase.class);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock del createUseCase para devolver un PayrollBatch válido
        when(createUseCase.create(any(CreatePayrollBatchCommand.class))).thenAnswer(invocation -> {
            CreatePayrollBatchCommand cmd = invocation.getArgument(0);

            // Convertir los comandos de pago en PayrollPayment con estado PENDING
            List<PayrollPayment> payments = cmd.payments().stream()
                    .map(p -> new PayrollPayment(
                            UUID.randomUUID(),
                            p.employeeDocumentType(),
                            p.employeeDocumentNumber(),
                            p.employeeFullName(),
                            p.bankCode(),
                            p.accountType(),
                            p.accountNumber(),
                            p.amount(),
                            PayrollPaymentStatus.PENDING
                    ))
                    .toList();

            BigDecimal totalAmount = payments.stream()
                    .map(PayrollPayment::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new PayrollBatch(
                    UUID.randomUUID(),         // id
                    cmd.companyId(),           // companyId
                    cmd.sourceAccountId(),     // sourceAccountId
                    cmd.currency(),            // currency
                    cmd.scheduledDate(),       // scheduledDate
                    PayrollBatchStatus.DRAFT,  // status
                    totalAmount,               // totalAmount
                    payments.size(),           // totalPayments
                    payments,                  // payments
                    OffsetDateTime.now(),      // createdAt
                    OffsetDateTime.now()       // updatedAt
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
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("2500000"));

        verify(repository).save(any());
    }
}

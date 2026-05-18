package com.corvian.payroll_payment_orchestrator.reconciliation.infrastructure;

import com.corvian.payroll_payment_orchestrator.reconciliation.domain.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "reconciliation_items")
public class ReconciliationItemEntity {
    @Id
    private UUID id;
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;
    @Column(name = "bank_reference", nullable = false, length = 120)
    private String bankReference;
    @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedAmount;
    @Column(name = "bank_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankAmount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationStatus status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

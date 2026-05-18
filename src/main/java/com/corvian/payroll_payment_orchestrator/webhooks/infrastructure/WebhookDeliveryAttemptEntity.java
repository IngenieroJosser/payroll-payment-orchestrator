package com.corvian.payroll_payment_orchestrator.webhooks.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "webhook_delivery_attempts")
public class WebhookDeliveryAttemptEntity {
    @Id
    private UUID id;

    @Column(name = "webhook_endpoint_id", nullable = false)
    private UUID webhookEndpointId;

    @Column(nullable = false, length = 120)
    private String event;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false)
    private Integer attempt;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

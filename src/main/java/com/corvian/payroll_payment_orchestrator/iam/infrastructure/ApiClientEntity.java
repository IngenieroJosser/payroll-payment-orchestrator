package com.corvian.payroll_payment_orchestrator.iam.infrastructure;

import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "api_clients")
public class ApiClientEntity {
    @Id
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "client_id", nullable = false, unique = true, length = 120)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 120)
    private String clientSecretHash;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 500)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApiClientStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;
}

package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.domain.BankConnectionStatus;
import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "bank_connections")
public class BankConnectionEntity {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "bank_code", nullable = false, length = 40)
    private String bankCode;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_token_encrypted", length = 2000)
    private String apiToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BankConnectionStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

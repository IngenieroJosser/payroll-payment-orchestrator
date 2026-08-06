package com.corvian.payroll_payment_orchestrator.companies.infrastructure;

import com.corvian.payroll_payment_orchestrator.payroll.domain.enums.AccountType;
import com.corvian.payroll_payment_orchestrator.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccountEntity {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number_encrypted", length = 2000)
    private String accountNumber;

    @Column(name = "account_number_hash", length = 128)
    private String accountNumberHash;

    @Column(name = "account_number_masked", nullable = false, length = 30)
    private String accountNumberMasked;

    @Column(name = "account_number_last4", nullable = false, length = 4)
    private String accountNumberLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BankAccountStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountNumberHash() { return accountNumberHash; }
    public void setAccountNumberHash(String accountNumberHash) { this.accountNumberHash = accountNumberHash; }
    public String getAccountNumberMasked() { return accountNumberMasked; }
    public void setAccountNumberMasked(String accountNumberMasked) { this.accountNumberMasked = accountNumberMasked; }
    public String getAccountNumberLast4() { return accountNumberLast4; }
    public void setAccountNumberLast4(String accountNumberLast4) { this.accountNumberLast4 = accountNumberLast4; }
    public BankAccountStatus getStatus() { return status; }
    public void setStatus(BankAccountStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

package com.corvian.payroll_payment_orchestrator.shared.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface JpaInboxMessageRepository extends JpaRepository<InboxMessageEntity, UUID> {
    Optional<InboxMessageEntity> findByMessageId(UUID messageId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InboxMessageEntity i where i.messageId=:messageId")
    Optional<InboxMessageEntity> findByMessageIdForUpdate(@Param("messageId") UUID messageId);
    long countByStatus(InboxStatus status);
}

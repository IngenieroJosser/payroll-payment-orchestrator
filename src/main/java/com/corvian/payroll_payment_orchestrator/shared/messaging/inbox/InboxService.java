package com.corvian.payroll_payment_orchestrator.shared.messaging.inbox;

import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class InboxService {
    private final JpaInboxMessageRepository repository;
    private final CryptoService cryptoService;
    private final Clock clock;

    public InboxService(JpaInboxMessageRepository repository, CryptoService cryptoService, Clock clock) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InboxBeginResult begin(UUID messageId, String messageType, String correlationId, String payload) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        var existing = repository.findByMessageIdForUpdate(messageId);
        if (existing.isPresent()) {
            InboxMessageEntity entity = existing.get();
            if (entity.getStatus() == InboxStatus.PROCESSED || entity.getStatus() == InboxStatus.DEAD) {
                return InboxBeginResult.ALREADY_PROCESSED;
            }
            if (entity.getStatus() == InboxStatus.PROCESSING
                    && entity.getUpdatedAt() != null
                    && entity.getUpdatedAt().isAfter(now.minusMinutes(6))) {
                return InboxBeginResult.IN_PROGRESS;
            }
            entity.setStatus(InboxStatus.PROCESSING);
            entity.setAttemptCount(entity.getAttemptCount() + 1);
            entity.setUpdatedAt(now);
            entity.setLastError(null);
            return InboxBeginResult.PROCESS;
        }
        InboxMessageEntity entity = new InboxMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setMessageId(messageId);
        entity.setMessageType(messageType);
        entity.setStatus(InboxStatus.PROCESSING);
        entity.setAttemptCount(1);
        entity.setCorrelationId(correlationId);
        entity.setPayloadHash(cryptoService.hmacSha256(payload));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            repository.saveAndFlush(entity);
            return InboxBeginResult.PROCESS;
        } catch (DataIntegrityViolationException race) {
            return InboxBeginResult.IN_PROGRESS;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID messageId) {
        InboxMessageEntity entity = repository.findByMessageIdForUpdate(messageId).orElseThrow();
        OffsetDateTime now = OffsetDateTime.now(clock);
        entity.setStatus(InboxStatus.PROCESSED);
        entity.setProcessedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastError(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID messageId, String error, boolean dead) {
        repository.findByMessageIdForUpdate(messageId).ifPresent(entity -> {
            entity.setStatus(dead ? InboxStatus.DEAD : InboxStatus.FAILED);
            entity.setLastError(sanitize(error));
            entity.setUpdatedAt(OffsetDateTime.now(clock));
        });
    }

    private String sanitize(String value) {
        if (value == null) return "Message processing failure";
        String s = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}

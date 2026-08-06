package com.corvian.payroll_payment_orchestrator.shared.messaging.outbox;

import com.corvian.payroll_payment_orchestrator.shared.filter.RequestMetadataContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OutboxService {
    private final JpaOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final RequestMetadataContext metadataContext;
    private final Clock clock;

    public OutboxService(JpaOutboxEventRepository repository, ObjectMapper objectMapper,
                         RequestMetadataContext metadataContext, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.metadataContext = metadataContext;
        this.clock = clock;
    }

    @Transactional
    public UUID enqueue(String aggregateType, UUID aggregateId, String eventType, int eventVersion,
                        String routingKey, Object payload) {
        try {
            OffsetDateTime now = OffsetDateTime.now(clock);
            OutboxEventEntity entity = new OutboxEventEntity();
            entity.setId(UUID.randomUUID());
            entity.setAggregateType(aggregateType);
            entity.setAggregateId(aggregateId);
            entity.setEventType(eventType);
            entity.setEventVersion(eventVersion);
            entity.setRoutingKey(routingKey);
            entity.setPayload(objectMapper.writeValueAsString(payload));
            entity.setCorrelationId(metadataContext.get().correlationId());
            entity.setStatus(OutboxStatus.PENDING);
            entity.setAttemptCount(0);
            entity.setNextAttemptAt(now);
            entity.setCreatedAt(now);
            repository.save(entity);
            return entity.getId();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize outbox event", ex);
        }
    }
}

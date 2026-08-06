package com.corvian.payroll_payment_orchestrator.shared.messaging.outbox;

import com.corvian.payroll_payment_orchestrator.shared.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final JpaOutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;

    public OutboxDispatcher(JpaOutboxEventRepository repository, RabbitTemplate rabbitTemplate, Clock clock,
                            @Value("${app.messaging.outbox-batch-size:50}") int batchSize,
                            @Value("${app.messaging.max-publish-attempts:10}") int maxAttempts) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox-fixed-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<OutboxEventEntity> events = repository.lockNextBatch(now, batchSize);
        for (OutboxEventEntity event : events) publish(event, now);
    }

    private void publish(OutboxEventEntity event, OffsetDateTime now) {
        try {
            event.setStatus(OutboxStatus.PUBLISHING);
            Message message = MessageBuilder.withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType("application/json")
                    .setMessageId(event.getId().toString())
                    .setCorrelationId(event.getCorrelationId())
                    .setHeader("eventType", event.getEventType())
                    .setHeader("eventVersion", event.getEventVersion())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();
            rabbitTemplate.invoke(operations -> {
                operations.send(RabbitMqConfig.PAYROLL_EXCHANGE, event.getRoutingKey(), message);
                operations.waitForConfirmsOrDie(5_000);
                return null;
            });
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(now);
            event.setLastError(null);
        } catch (Exception ex) {
            int attempts = event.getAttemptCount() + 1;
            event.setAttemptCount(attempts);
            event.setLastError(sanitize(ex.getMessage()));
            if (attempts >= maxAttempts) {
                event.setStatus(OutboxStatus.DEAD);
                log.error("Outbox event exhausted retries. eventId={}, type={}", event.getId(), event.getEventType());
            } else {
                event.setStatus(OutboxStatus.RETRY);
                event.setNextAttemptAt(now.plus(backoff(attempts)));
                log.warn("Outbox event publish failed; scheduled retry. eventId={}, attempt={}", event.getId(), attempts);
            }
        }
    }

    private Duration backoff(int attempt) {
        long seconds = Math.min(300, (long) Math.pow(2, Math.min(attempt, 8)));
        return Duration.ofSeconds(seconds);
    }

    private String sanitize(String value) {
        if (value == null) return "Publish failure";
        String s = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}

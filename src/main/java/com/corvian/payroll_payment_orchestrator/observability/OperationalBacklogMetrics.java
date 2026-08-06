package com.corvian.payroll_payment_orchestrator.observability;

import com.corvian.payroll_payment_orchestrator.banks.application.model.BankSubmissionStatus;
import com.corvian.payroll_payment_orchestrator.banks.infrastructure.submission.JpaBankSubmissionRepository;
import com.corvian.payroll_payment_orchestrator.shared.messaging.inbox.InboxStatus;
import com.corvian.payroll_payment_orchestrator.shared.messaging.inbox.JpaInboxMessageRepository;
import com.corvian.payroll_payment_orchestrator.shared.messaging.outbox.JpaOutboxEventRepository;
import com.corvian.payroll_payment_orchestrator.shared.messaging.outbox.OutboxStatus;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.JpaWebhookDeliveryAttemptRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OperationalBacklogMetrics {
    private static final Logger log = LoggerFactory.getLogger(OperationalBacklogMetrics.class);
    private static final String[] WEBHOOK_STATUSES = {"PENDING", "SENDING", "RETRY_PENDING", "FAILED", "DELIVERED"};

    private final JpaOutboxEventRepository outboxRepository;
    private final JpaInboxMessageRepository inboxRepository;
    private final JpaBankSubmissionRepository bankSubmissionRepository;
    private final JpaWebhookDeliveryAttemptRepository webhookRepository;
    private final Map<OutboxStatus, AtomicLong> outbox = new EnumMap<>(OutboxStatus.class);
    private final Map<InboxStatus, AtomicLong> inbox = new EnumMap<>(InboxStatus.class);
    private final Map<BankSubmissionStatus, AtomicLong> bankSubmissions = new EnumMap<>(BankSubmissionStatus.class);
    private final Map<String, AtomicLong> webhooks = new LinkedHashMap<>();

    public OperationalBacklogMetrics(
            MeterRegistry registry,
            JpaOutboxEventRepository outboxRepository,
            JpaInboxMessageRepository inboxRepository,
            JpaBankSubmissionRepository bankSubmissionRepository,
            JpaWebhookDeliveryAttemptRepository webhookRepository
    ) {
        this.outboxRepository = outboxRepository;
        this.inboxRepository = inboxRepository;
        this.bankSubmissionRepository = bankSubmissionRepository;
        this.webhookRepository = webhookRepository;

        for (OutboxStatus status : OutboxStatus.values()) {
            register(registry, "payroll.outbox.events", "status", status.name(), outbox, status);
        }
        for (InboxStatus status : InboxStatus.values()) {
            register(registry, "payroll.inbox.messages", "status", status.name(), inbox, status);
        }
        for (BankSubmissionStatus status : BankSubmissionStatus.values()) {
            register(registry, "payroll.bank.submissions.current", "status", status.name(), bankSubmissions, status);
        }
        for (String status : WEBHOOK_STATUSES) {
            AtomicLong value = new AtomicLong();
            webhooks.put(status, value);
            Gauge.builder("payroll.webhook.deliveries.current", value, AtomicLong::get)
                    .tag("status", status)
                    .description("Persisted webhook delivery attempts by status")
                    .register(registry);
        }
    }

    @Scheduled(fixedDelayString = "${app.observability.backlog-refresh-ms:30000}")
    public void refresh() {
        try {
            outbox.forEach((status, value) -> value.set(outboxRepository.countByStatus(status)));
            inbox.forEach((status, value) -> value.set(inboxRepository.countByStatus(status)));
            bankSubmissions.forEach((status, value) -> value.set(bankSubmissionRepository.countByStatus(status)));
            webhooks.forEach((status, value) -> value.set(webhookRepository.countByStatus(status)));
        } catch (RuntimeException ex) {
            log.warn("Unable to refresh operational backlog metrics", ex);
        }
    }

    private <T extends Enum<T>> void register(
            MeterRegistry registry,
            String metric,
            String tagName,
            String tagValue,
            Map<T, AtomicLong> target,
            T key
    ) {
        AtomicLong value = new AtomicLong();
        target.put(key, value);
        Gauge.builder(metric, value, AtomicLong::get)
                .tag(tagName, tagValue)
                .description("Persisted operational backlog by status")
                .register(registry);
    }
}

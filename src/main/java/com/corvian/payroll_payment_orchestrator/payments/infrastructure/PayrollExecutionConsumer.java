package com.corvian.payroll_payment_orchestrator.payments.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankSubmissionCoordinator;
import com.corvian.payroll_payment_orchestrator.payroll.infrastructure.messaging.PayrollExecutionMessage;
import com.corvian.payroll_payment_orchestrator.shared.config.RabbitMqConfig;
import com.corvian.payroll_payment_orchestrator.shared.messaging.inbox.InboxBeginResult;
import com.corvian.payroll_payment_orchestrator.shared.messaging.inbox.InboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Component
public class PayrollExecutionConsumer {
    private static final Logger log = LoggerFactory.getLogger(PayrollExecutionConsumer.class);
    private static final int MAX_DELIVERIES = 20;

    private final BankSubmissionCoordinator coordinator;
    private final InboxService inboxService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public PayrollExecutionConsumer(BankSubmissionCoordinator coordinator, InboxService inboxService,
                                    ObjectMapper objectMapper, RabbitTemplate rabbitTemplate) {
        this.coordinator = coordinator;
        this.inboxService = inboxService;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYROLL_EXECUTION_QUEUE)
    public void consume(Message amqpMessage) {
        String payload = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        PayrollExecutionMessage message;
        try {
            message = parse(payload, amqpMessage);
        } catch (Exception ex) {
            log.error("Discarding malformed payroll execution message. messageId={}", amqpMessage.getMessageProperties().getMessageId());
            sendToFinalDlq(amqpMessage);
            return;
        }

        InboxBeginResult beginResult = inboxService.begin(
                message.messageId(), "PAYROLL_EXECUTION_REQUESTED", message.correlationId(), payload);
        if (beginResult == InboxBeginResult.ALREADY_PROCESSED) {
            log.info("Skipping already processed payroll execution message. messageId={}", message.messageId());
            return;
        }
        if (beginResult == InboxBeginResult.IN_PROGRESS) {
            throw new AmqpRejectAndDontRequeueException("Payroll execution message is already being processed");
        }

        try {
            coordinator.submit(message.batchId(), message.messageId(), message.correlationId());
            inboxService.complete(message.messageId());
            log.info("Payroll execution message processed. messageId={}, batchId={}", message.messageId(), message.batchId());
        } catch (Exception ex) {
            long delivery = deliveryCount(amqpMessage);
            boolean dead = delivery >= MAX_DELIVERIES;
            inboxService.fail(message.messageId(), ex.getMessage(), dead);
            if (dead) {
                log.error("Payroll execution exhausted retries. messageId={}, batchId={}", message.messageId(), message.batchId(), ex);
                sendToFinalDlq(amqpMessage);
                return;
            }
            log.warn("Payroll execution failed; message will be retried. messageId={}, delivery={}", message.messageId(), delivery, ex);
            throw new AmqpRejectAndDontRequeueException("Retryable payroll execution failure", ex);
        }
    }

    private PayrollExecutionMessage parse(String payload, Message amqpMessage) throws Exception {
        String trimmed = payload.trim();
        if (trimmed.startsWith("{")) {
            PayrollExecutionMessage parsed = objectMapper.readValue(trimmed, PayrollExecutionMessage.class);
            if (parsed.messageId() == null || parsed.batchId() == null || parsed.version() != 1) {
                throw new IllegalArgumentException("Unsupported or incomplete payroll execution message");
            }
            return parsed;
        }
        UUID batchId = UUID.fromString(trimmed.replace("\"", ""));
        String brokerId = amqpMessage.getMessageProperties().getMessageId();
        UUID messageId;
        try { messageId = brokerId == null ? UUID.nameUUIDFromBytes(amqpMessage.getBody()) : UUID.fromString(brokerId); }
        catch (Exception ignored) { messageId = UUID.nameUUIDFromBytes(amqpMessage.getBody()); }
        return new PayrollExecutionMessage(messageId, 1, batchId, OffsetDateTime.now(ZoneOffset.UTC),
                amqpMessage.getMessageProperties().getCorrelationId());
    }

    private long deliveryCount(Message message) {
        long count = 1;
        var xDeaths = message.getMessageProperties().getXDeathHeader();
        if (xDeaths != null) {
            for (Map<String, ?> death : xDeaths) {
                Object value = death.get("count");
                if (value instanceof Number number) count += number.longValue();
            }
        }
        return count;
    }

    private void sendToFinalDlq(Message message) {
        rabbitTemplate.send(RabbitMqConfig.PAYROLL_DLQ_EXCHANGE, RabbitMqConfig.PAYROLL_EXECUTION_ROUTING_KEY, message);
    }
}

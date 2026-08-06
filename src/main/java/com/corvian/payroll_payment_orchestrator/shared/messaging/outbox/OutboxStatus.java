package com.corvian.payroll_payment_orchestrator.shared.messaging.outbox;
public enum OutboxStatus { PENDING, PUBLISHING, PUBLISHED, RETRY, DEAD }

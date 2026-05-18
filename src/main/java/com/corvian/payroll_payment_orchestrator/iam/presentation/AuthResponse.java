package com.corvian.payroll_payment_orchestrator.iam.presentation;
import java.util.List;
public record AuthResponse(String tokenType, String accessToken, long expiresInSeconds, List<String> authorities) {}

package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import com.corvian.payroll_payment_orchestrator.idempotency.application.IdempotencyService;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH");
    private final IdempotencyService service;
    private final SecurityProperties properties;

    public IdempotencyFilter(IdempotencyService service, SecurityProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !properties.isRequireIdempotencyKey()
                || !METHODS.contains(request.getMethod())
                || uri.contains("/auth/login")
                || uri.contains("/oauth/token")
                || uri.contains("/webhooks")
                || uri.contains("/iam");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"IDEMPOTENCY_KEY_REQUIRED\",\"message\":\"Idempotency-Key header is required for mutating requests\",\"details\":[]}}");
            return;
        }
        String fingerprint = request.getMethod() + " " + request.getRequestURI() + "?" + (request.getQueryString() == null ? "" : request.getQueryString());
        service.registerOrReject(key, request.getMethod() + ":" + request.getRequestURI(), fingerprint);
        filterChain.doFilter(request, response);
    }
}

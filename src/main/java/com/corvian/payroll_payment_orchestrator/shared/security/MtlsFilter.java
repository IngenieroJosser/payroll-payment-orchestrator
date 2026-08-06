package com.corvian.payroll_payment_orchestrator.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Instant;

@Component
public class MtlsFilter extends OncePerRequestFilter {
    private final SecurityProperties properties;

    public MtlsFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isMtlsEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certificates == null || certificates.length == 0 || !isAuthorized(certificates[0])) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"CLIENT_CERTIFICATE_REQUIRED\",\"message\":\"A valid and authorized client certificate is required\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(X509Certificate certificate) {
        try {
            certificate.checkValidity(java.util.Date.from(Instant.now()));
            if (properties.getMtlsAllowedSubjects().isEmpty()) return true;
            String subject = certificate.getSubjectX500Principal().getName();
            return properties.getMtlsAllowedSubjects().contains(subject);
        } catch (Exception ex) {
            return false;
        }
    }
}

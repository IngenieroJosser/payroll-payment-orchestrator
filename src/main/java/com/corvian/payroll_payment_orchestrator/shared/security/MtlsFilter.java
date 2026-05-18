package com.corvian.payroll_payment_orchestrator.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;

@Component
public class MtlsFilter extends OncePerRequestFilter {
    private final SecurityProperties properties;

    public MtlsFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isMtlsEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Client certificate is required");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

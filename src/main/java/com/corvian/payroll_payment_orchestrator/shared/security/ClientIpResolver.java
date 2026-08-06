package com.corvian.payroll_payment_orchestrator.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    private final SecurityProperties properties;

    public ClientIpResolver(SecurityProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!IpNetworkMatcher.matchesAny(remoteAddress, properties.getTrustedProxyAddresses())) {
            return remoteAddress;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddress;
        }
        String candidate = forwarded.split(",", 2)[0].trim();
        return IpNetworkMatcher.isLiteralAddress(candidate) ? candidate : remoteAddress;
    }
}

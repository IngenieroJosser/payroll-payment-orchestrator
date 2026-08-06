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

@Component
public class IpAllowlistFilter extends OncePerRequestFilter {
    private final SecurityProperties properties;
    private final ClientIpResolver clientIpResolver;

    public IpAllowlistFilter(SecurityProperties properties, ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
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
        if (!properties.isIpAllowlistEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String ip = clientIpResolver.resolve(request);
        if (!IpNetworkMatcher.matchesAny(ip, properties.getIpAllowlist())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"IP_ACCESS_DENIED\",\"message\":\"Client IP address is not authorized\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

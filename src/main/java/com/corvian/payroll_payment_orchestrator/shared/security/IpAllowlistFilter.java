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

    public IpAllowlistFilter(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isIpAllowlistEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String ip = clientIp(request);
        if (!properties.getIpAllowlist().contains(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String json = "{\n" +
                    "  \"success\": false,\n" +
                    "  \"error\": {\n" +
                    "    \"code\": \"IP_ACCESS_DENIED\",\n" +
                    "    \"message\": \"Access denied: client IP address is not authorized in the IP allowlist.\",\n" +
                    "    \"details\": [\n" +
                    "      \"Client IP: " + ip + "\"\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}";
            response.getWriter().write(json);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

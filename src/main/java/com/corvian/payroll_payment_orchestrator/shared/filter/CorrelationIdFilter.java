package com.corvian.payroll_payment_orchestrator.shared.filter;

import com.corvian.payroll_payment_orchestrator.shared.security.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    private final RequestMetadataContext requestMetadataContext;
    private final ClientIpResolver clientIpResolver;

    public CorrelationIdFilter(RequestMetadataContext requestMetadataContext, ClientIpResolver clientIpResolver) {
        this.requestMetadataContext = requestMetadataContext;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requested = request.getHeader(HEADER);
        String correlationId = requested != null && SAFE_ID.matcher(requested).matches()
                ? requested
                : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        requestMetadataContext.set(correlationId, clientIpResolver.resolve(request));
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            requestMetadataContext.clear();
            MDC.remove(MDC_KEY);
        }
    }
}

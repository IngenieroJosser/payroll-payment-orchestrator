package com.corvian.payroll_payment_orchestrator.shared.security;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final SecurityProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rate-limit-cleaner");
        thread.setDaemon(true);
        return thread;
    });

    public RateLimitFilter(SecurityProperties properties, ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        scheduler.scheduleAtFixedRate(this::pruneExpiredBuckets, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/health") || path.contains("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        long windowMs = Duration.ofSeconds(properties.getRateLimitWindowSeconds()).toMillis();
        int limit = properties.getRateLimitRequests();
        String key = clientKey(request);
        RateLimitBucket bucket = buckets.computeIfAbsent(key, ignored -> new RateLimitBucket());
        int observed = bucket.registerAndCount(now, windowMs);
        int remaining = Math.max(0, limit - observed);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(properties.getRateLimitWindowSeconds()));
        if (observed > limit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(properties.getRateLimitWindowSeconds()));
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Request rate limit exceeded\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "principal:" + authentication.getName();
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private void pruneExpiredBuckets() {
        long threshold = System.currentTimeMillis() - Duration.ofSeconds(properties.getRateLimitWindowSeconds() * 2L).toMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessTime < threshold);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    private static final class RateLimitBucket {
        private final Queue<Long> timestamps = new ConcurrentLinkedQueue<>();
        private volatile long lastAccessTime = System.currentTimeMillis();

        synchronized int registerAndCount(long now, long windowMs) {
            lastAccessTime = now;
            long threshold = now - windowMs;
            while (!timestamps.isEmpty() && timestamps.peek() < threshold) timestamps.poll();
            timestamps.add(now);
            return timestamps.size();
        }
    }
}

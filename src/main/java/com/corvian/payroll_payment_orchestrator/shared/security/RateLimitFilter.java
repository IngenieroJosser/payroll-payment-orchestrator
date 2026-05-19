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
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rate-limit-cleaner");
        thread.setDaemon(true);
        return thread;
    });

    public RateLimitFilter() {
        // Schedule cleanup every 1 minute
        this.scheduler.scheduleAtFixedRate(this::pruneExpiredBuckets, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for Swagger UI, API Docs, and Health endpoints
        String path = request.getRequestURI();
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs") || path.contains("/health") || path.contains("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Identify client by token principal or fallback to IP
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key;
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        if (isAuthenticated) {
            key = "token:" + auth.getName();
        } else {
            key = "ip:" + clientIp(request);
        }

        long now = System.currentTimeMillis();
        long windowMs = 5000; // 5 seconds
        int hardLimit = 20;   // Max 20 requests in 5 seconds
        int softLimit = 10;   // Start queueing/throttling at 10 requests

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket());
        int currentRequests = bucket.getRequestsInWindow(now, windowMs);

        // Flood Protection: Limit exceeded completely
        if (currentRequests >= hardLimit) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(hardLimit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", "5");
            response.setHeader("X-RateLimit-Throttled", "false");

            response.getWriter().write("{"
                    + "\"success\":false,"
                    + "\"error\":{"
                    + "\"code\":\"RATE_LIMIT_EXCEEDED\","
                    + "\"message\":\"Rate limit of 20 requests per 5 seconds exceeded. Flood protection active.\","
                    + "\"details\":[\"Requests in last 5 seconds: " + currentRequests + "\"]"
                    + "}"
                    + "}");
            return;
        }

        // Register this request
        bucket.addRequest(now);
        int remaining = hardLimit - (currentRequests + 1);

        // Throttling/Queueing logic: between 10 and 20 requests
        boolean throttled = false;
        long delayMs = 0;
        if (currentRequests >= softLimit) {
            throttled = true;
            // Throttle requests by slowing them down proportionally to prevent downstream system crash
            int excess = currentRequests - softLimit + 1;
            delayMs = excess * 250L; // Delays range from 250ms up to 2500ms

            logger.warn("RateLimit queueing active for " + key + ". Delaying thread by " + delayMs + "ms to prevent system stress.");

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Set rate limit metrics headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(hardLimit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
        response.setHeader("X-RateLimit-Reset", "5");
        response.setHeader("X-RateLimit-Throttled", String.valueOf(throttled));
        if (throttled) {
            response.setHeader("X-RateLimit-Throttle-Delay-Ms", String.valueOf(delayMs));
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

    private void pruneExpiredBuckets() {
        long now = System.currentTimeMillis();
        // Remove buckets inactive for more than 30 seconds
        buckets.entrySet().removeIf(entry -> now - entry.getValue().getLastAccessTime() > 30_000);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    private static class RateLimitBucket {
        private final Queue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
        private volatile long lastAccessTime = System.currentTimeMillis();

        public synchronized int getRequestsInWindow(long nowMs, long windowMs) {
            lastAccessTime = nowMs;
            long threshold = nowMs - windowMs;
            while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < threshold) {
                requestTimestamps.poll();
            }
            return requestTimestamps.size();
        }

        public synchronized void addRequest(long nowMs) {
            requestTimestamps.add(nowMs);
            lastAccessTime = nowMs;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}

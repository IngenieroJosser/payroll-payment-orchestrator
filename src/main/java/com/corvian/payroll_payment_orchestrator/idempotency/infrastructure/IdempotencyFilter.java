package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import com.corvian.payroll_payment_orchestrator.idempotency.application.port.IdempotencyStorePort;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH");
    private final IdempotencyStorePort storePort;
    private final CryptoService cryptoService;
    private final SecurityProperties properties;

    public IdempotencyFilter(IdempotencyStorePort storePort, CryptoService cryptoService, SecurityProperties properties) {
        this.storePort = storePort;
        this.cryptoService = cryptoService;
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

        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String bodyStr = new String(wrappedRequest.getCachedBody(), StandardCharsets.UTF_8);
        String fingerprint = request.getMethod() + " " + request.getRequestURI() + "?" + 
                (request.getQueryString() == null ? "" : request.getQueryString()) + "\nBody: " + bodyStr;
        String requestHash = cryptoService.hmacSha256(fingerprint);

        // Intentar leer respuesta existente (Replay)
        String cachedResponse = storePort.getResponse(key, endpoint);
        if (cachedResponse != null) {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(cachedResponse);
            return;
        }

        // Adquirir bloqueo idempotente
        try {
            boolean locked = storePort.lock(key, endpoint, requestHash);
            if (!locked) {
                response.setStatus(409);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"CONCURRENT_REQUEST\",\"message\":\"Another request with this Idempotency-Key is already in progress\",\"details\":[]}}");
                return;
            }
        } catch (DomainException ex) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(String.format("{\"success\":false,\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"details\":[]}}", ex.getCode(), ex.getMessage()));
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            int status = wrappedResponse.getStatus();
            if (status >= 200 && status < 300) {
                byte[] responseBody = wrappedResponse.getContentAsByteArray();
                String responseBodyStr = new String(responseBody, StandardCharsets.UTF_8);
                storePort.saveResponse(key, endpoint, responseBodyStr, 24);
            } else {
                storePort.unlock(key, endpoint);
            }
            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            storePort.unlock(key, endpoint);
            throw ex;
        }
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            InputStream requestInputStream = request.getInputStream();
            this.cachedBody = requestInputStream.readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(this.cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new BufferedReader(new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8));
        }

        public byte[] getCachedBody() {
            return this.cachedBody;
        }
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream cachedBodyInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return cachedBodyInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            return cachedBodyInputStream.read();
        }
    }
}


package com.corvian.payroll_payment_orchestrator.idempotency.infrastructure;

import com.corvian.payroll_payment_orchestrator.idempotency.application.port.IdempotencyStorePort;
import com.corvian.payroll_payment_orchestrator.idempotency.application.port.StoredIdempotencyResponse;
import com.corvian.payroll_payment_orchestrator.shared.crypto.CryptoService;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int MAX_REQUEST_BYTES = 2 * 1024 * 1024;
    private static final int MAX_CACHED_RESPONSE_BYTES = 2 * 1024 * 1024;
    private final IdempotencyStorePort storePort;
    private final CryptoService cryptoService;
    private final SecurityProperties properties;
    private final ActorContext actorContext;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyStorePort storePort, CryptoService cryptoService, SecurityProperties properties,
                             ActorContext actorContext, ObjectMapper objectMapper) {
        this.storePort = storePort; this.cryptoService = cryptoService; this.properties = properties;
        this.actorContext = actorContext; this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !properties.isRequireIdempotencyKey() || !METHODS.contains(request.getMethod())
                || uri.equals("/api/v1/auth/login") || uri.equals("/api/v1/oauth/token");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || !key.matches("^[A-Za-z0-9._:-]{8,120}$")) {
            writeError(response, 400, "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key must contain 8 to 120 safe characters");
            return;
        }
        if (request.getContentLengthLong() > MAX_REQUEST_BYTES) {
            writeError(response, 413, "REQUEST_TOO_LARGE", "Request body exceeds the idempotency processing limit");
            return;
        }

        CachedBodyHttpServletRequest wrapped;
        try { wrapped = new CachedBodyHttpServletRequest(request, MAX_REQUEST_BYTES); }
        catch (BodyTooLargeException ex) {
            writeError(response, 413, "REQUEST_TOO_LARGE", "Request body exceeds the idempotency processing limit");
            return;
        }
        String endpoint = scopedEndpoint(request);
        String body = new String(wrapped.cachedBody, StandardCharsets.UTF_8);
        String fingerprint = request.getMethod() + " " + request.getRequestURI() + "?"
                + (request.getQueryString() == null ? "" : request.getQueryString()) + "\n" + body;
        String hash = cryptoService.hmacSha256(fingerprint);

        try {
            StoredIdempotencyResponse cached = storePort.getStoredResponse(key, endpoint, hash);
            if (cached != null) {
                response.setStatus(cached.status());
                response.setContentType(cached.contentType());
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("Idempotent-Replayed", "true");
                response.getWriter().write(cached.body());
                return;
            }
            if (!storePort.lock(key, endpoint, hash)) {
                writeError(response, 409, "IDEMPOTENT_REQUEST_IN_PROGRESS", "The same idempotent operation is already in progress");
                return;
            }
        } catch (DomainException ex) {
            writeError(response, 409, ex.getCode(), ex.getMessage());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(wrapped, wrappedResponse);
            int status = wrappedResponse.getStatus();
            byte[] responseBody = wrappedResponse.getContentAsByteArray();
            if (responseBody.length <= MAX_CACHED_RESPONSE_BYTES) {
                storePort.saveResponse(key, endpoint, status,
                        wrappedResponse.getContentType() == null ? MediaType.APPLICATION_JSON_VALUE : wrappedResponse.getContentType(),
                        new String(responseBody, StandardCharsets.UTF_8), 24);
            } else {
                storePort.unlock(key, endpoint);
            }
            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            storePort.unlock(key, endpoint);
            throw ex;
        }
    }

    private String scopedEndpoint(HttpServletRequest request) {
        var actor = actorContext.current();
        String scope = String.valueOf(actor.tenantId()) + "|" + actor.companyId() + "|" + actor.subject();
        String scopeHash = cryptoService.hmacSha256(scope).substring(0, 24);
        return scopeHash + ":" + request.getMethod() + ":" + request.getRequestURI();
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;
        private CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
            super(request);
            try (InputStream input = request.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int read; int total = 0;
                while ((read = input.read(buffer)) != -1) {
                    total += read; if (total > maxBytes) throw new BodyTooLargeException();
                    output.write(buffer, 0, read);
                }
                this.cachedBody = output.toByteArray();
            }
        }
        @Override public ServletInputStream getInputStream() { return new CachedBodyServletInputStream(cachedBody); }
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;
        private CachedBodyServletInputStream(byte[] body) { input = new ByteArrayInputStream(body); }
        @Override public boolean isFinished() { return input.available() == 0; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(ReadListener listener) { if (listener != null) { try { listener.onDataAvailable(); } catch (IOException ex) { listener.onError(ex); } } }
        @Override public int read() { return input.read(); }
    }
    private static final class BodyTooLargeException extends IOException {}
}

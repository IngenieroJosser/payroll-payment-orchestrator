package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {
    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String issueToken(String subject, String tokenType, List<String> authorities) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", subject);
            payload.put("typ", tokenType);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(properties.getJwtExpirationMinutes() * 60).getEpochSecond());
            payload.put("authorities", authorities);

            String unsigned = base64Url(objectMapper.writeValueAsBytes(header)) + "." + base64Url(objectMapper.writeValueAsBytes(payload));
            return unsigned + "." + sign(unsigned);
        } catch (Exception ex) {
            throw new DomainException("JWT_ISSUE_FAILED", "Could not issue JWT token");
        }
    }

    public JwtPrincipal parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
            Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {});
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalArgumentException("Expired JWT");
            }
            String subject = String.valueOf(payload.get("sub"));
            String type = String.valueOf(payload.get("typ"));
            List<String> authorities = ((List<?>) payload.getOrDefault("authorities", List.of())).stream().map(String::valueOf).toList();
            return new JwtPrincipal(subject, type, authorities);
        } catch (Exception ex) {
            throw new DomainException("INVALID_JWT", "Invalid or expired JWT token");
        }
    }

    private String sign(String unsigned) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) return false;
        int result = 0;
        for (int i = 0; i < left.length(); i++) result |= left.charAt(i) ^ right.charAt(i);
        return result == 0;
    }

    public record JwtPrincipal(String subject, String type, List<String> authorities) {}
}

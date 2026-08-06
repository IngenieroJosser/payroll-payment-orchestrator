package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private static final String ALGORITHM = "HS256";

    private final SecurityProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(SecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String issueToken(String subject, String tokenType, List<String> authorities) {
        return issueToken(subject, tokenType, authorities, null, null, false);
    }

    public String issueToken(
            String subject,
            String tokenType,
            List<String> authorities,
            UUID tenantId,
            UUID companyId,
            boolean platformAdmin
    ) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", ALGORITHM);
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", properties.getJwtIssuer());
            payload.put("aud", properties.getJwtAudience());
            payload.put("sub", subject);
            payload.put("typ", tokenType);
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("iat", now.getEpochSecond());
            payload.put("nbf", now.minusSeconds(1).getEpochSecond());
            payload.put("exp", now.plusSeconds(expirationSeconds()).getEpochSecond());
            payload.put("authorities", authorities == null ? List.of() : authorities);
            payload.put("platform_admin", platformAdmin);
            if (tenantId != null) payload.put("tenant_id", tenantId.toString());
            if (companyId != null) payload.put("company_id", companyId.toString());

            String unsigned = base64Url(objectMapper.writeValueAsBytes(header)) + "." + base64Url(objectMapper.writeValueAsBytes(payload));
            return unsigned + "." + sign(unsigned);
        } catch (Exception ex) {
            throw new DomainException("JWT_ISSUE_FAILED", "Could not issue JWT token");
        }
    }

    public JwtPrincipal parseAndValidate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");

            Map<String, Object> header = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), new TypeReference<>() {});
            if (!ALGORITHM.equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
                throw new IllegalArgumentException("Unsupported JWT header");
            }

            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }

            Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {});
            Instant now = Instant.now();
            long skew = properties.getJwtClockSkewSeconds();
            long exp = number(payload, "exp");
            long nbf = number(payload, "nbf");
            long iat = number(payload, "iat");
            if (now.getEpochSecond() - skew >= exp) throw new IllegalArgumentException("Expired JWT");
            if (now.getEpochSecond() + skew < nbf) throw new IllegalArgumentException("JWT is not active");
            if (iat > now.getEpochSecond() + skew) throw new IllegalArgumentException("JWT issued in the future");
            if (!properties.getJwtIssuer().equals(String.valueOf(payload.get("iss")))) throw new IllegalArgumentException("Invalid issuer");
            if (!properties.getJwtAudience().equals(String.valueOf(payload.get("aud")))) throw new IllegalArgumentException("Invalid audience");

            String subject = requiredText(payload, "sub");
            String type = requiredText(payload, "typ");
            String jti = requiredText(payload, "jti");
            List<String> authorities = ((List<?>) payload.getOrDefault("authorities", List.of())).stream().map(String::valueOf).toList();
            UUID tenantId = optionalUuid(payload.get("tenant_id"));
            UUID companyId = optionalUuid(payload.get("company_id"));
            boolean platformAdmin = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("platform_admin", false)));
            return new JwtPrincipal(subject, type, authorities, tenantId, companyId, platformAdmin, jti);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("INVALID_JWT", "Invalid or expired JWT token");
        }
    }

    public long expirationSeconds() {
        return properties.getJwtExpirationMinutes() * 60;
    }

    private long number(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Missing numeric claim: " + key);
        return number.longValue();
    }

    private String requiredText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException("Missing claim: " + key);
        return String.valueOf(value);
    }

    private UUID optionalUuid(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : UUID.fromString(String.valueOf(value));
    }

    private String sign(String unsigned) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return base64Url(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public record JwtPrincipal(
            String subject,
            String type,
            List<String> authorities,
            UUID tenantId,
            UUID companyId,
            boolean platformAdmin,
            String jti
    ) {}
}

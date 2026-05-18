package com.corvian.payroll_payment_orchestrator.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private String jwtSecret = "CHANGE_ME_DEV_SECRET_CHANGE_ME_DEV_SECRET_32_BYTES_MIN";
    private long jwtExpirationMinutes = 60;
    private boolean requireIdempotencyKey = true;
    private boolean ipAllowlistEnabled = false;
    private List<String> ipAllowlist = new ArrayList<>();
    private boolean mtlsEnabled = false;
    private int rateLimitRequests = 120;
    private int rateLimitWindowSeconds = 60;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public long getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(long jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }
    public boolean isRequireIdempotencyKey() { return requireIdempotencyKey; }
    public void setRequireIdempotencyKey(boolean requireIdempotencyKey) { this.requireIdempotencyKey = requireIdempotencyKey; }
    public boolean isIpAllowlistEnabled() { return ipAllowlistEnabled; }
    public void setIpAllowlistEnabled(boolean ipAllowlistEnabled) { this.ipAllowlistEnabled = ipAllowlistEnabled; }
    public List<String> getIpAllowlist() { return ipAllowlist; }
    public void setIpAllowlist(List<String> ipAllowlist) { this.ipAllowlist = ipAllowlist; }
    public boolean isMtlsEnabled() { return mtlsEnabled; }
    public void setMtlsEnabled(boolean mtlsEnabled) { this.mtlsEnabled = mtlsEnabled; }
    public int getRateLimitRequests() { return rateLimitRequests; }
    public void setRateLimitRequests(int rateLimitRequests) { this.rateLimitRequests = rateLimitRequests; }
    public int getRateLimitWindowSeconds() { return rateLimitWindowSeconds; }
    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) { this.rateLimitWindowSeconds = rateLimitWindowSeconds; }
}

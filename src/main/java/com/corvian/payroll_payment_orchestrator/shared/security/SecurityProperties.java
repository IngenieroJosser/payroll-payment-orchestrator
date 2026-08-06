package com.corvian.payroll_payment_orchestrator.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private String jwtSecret;
    private String jwtIssuer = "payroll-payment-orchestrator";
    private String jwtAudience = "payroll-api";
    private long jwtExpirationMinutes = 60;
    private long jwtClockSkewSeconds = 30;
    private boolean requireIdempotencyKey = true;
    private boolean ipAllowlistEnabled;
    private List<String> ipAllowlist = new ArrayList<>();
    private List<String> trustedProxyAddresses = new ArrayList<>();
    private boolean mtlsEnabled;
    private List<String> mtlsAllowedSubjects = new ArrayList<>();
    private int rateLimitRequests = 120;
    private int rateLimitWindowSeconds = 60;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public String getJwtIssuer() { return jwtIssuer; }
    public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }
    public String getJwtAudience() { return jwtAudience; }
    public void setJwtAudience(String jwtAudience) { this.jwtAudience = jwtAudience; }
    public long getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(long jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }
    public long getJwtClockSkewSeconds() { return jwtClockSkewSeconds; }
    public void setJwtClockSkewSeconds(long jwtClockSkewSeconds) { this.jwtClockSkewSeconds = jwtClockSkewSeconds; }
    public boolean isRequireIdempotencyKey() { return requireIdempotencyKey; }
    public void setRequireIdempotencyKey(boolean requireIdempotencyKey) { this.requireIdempotencyKey = requireIdempotencyKey; }
    public boolean isIpAllowlistEnabled() { return ipAllowlistEnabled; }
    public void setIpAllowlistEnabled(boolean ipAllowlistEnabled) { this.ipAllowlistEnabled = ipAllowlistEnabled; }
    public List<String> getIpAllowlist() { return ipAllowlist; }
    public void setIpAllowlist(List<String> ipAllowlist) { this.ipAllowlist = ipAllowlist; }
    public List<String> getTrustedProxyAddresses() { return trustedProxyAddresses; }
    public void setTrustedProxyAddresses(List<String> trustedProxyAddresses) { this.trustedProxyAddresses = trustedProxyAddresses; }
    public boolean isMtlsEnabled() { return mtlsEnabled; }
    public void setMtlsEnabled(boolean mtlsEnabled) { this.mtlsEnabled = mtlsEnabled; }
    public List<String> getMtlsAllowedSubjects() { return mtlsAllowedSubjects; }
    public void setMtlsAllowedSubjects(List<String> mtlsAllowedSubjects) { this.mtlsAllowedSubjects = mtlsAllowedSubjects; }
    public int getRateLimitRequests() { return rateLimitRequests; }
    public void setRateLimitRequests(int rateLimitRequests) { this.rateLimitRequests = rateLimitRequests; }
    public int getRateLimitWindowSeconds() { return rateLimitWindowSeconds; }
    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) { this.rateLimitWindowSeconds = rateLimitWindowSeconds; }
}

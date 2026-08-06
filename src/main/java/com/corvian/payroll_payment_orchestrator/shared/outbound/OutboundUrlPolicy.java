package com.corvian.payroll_payment_orchestrator.shared.outbound;

import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

@Component
public class OutboundUrlPolicy {
    private final OutboundUrlProperties properties;

    public OutboundUrlPolicy(OutboundUrlProperties properties) {
        this.properties = properties;
    }

    public URI validate(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl == null ? "" : rawUrl.trim());
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalid();
            }
            String scheme = uri.getScheme().toLowerCase();
            if (!"https".equals(scheme) && !(properties.isAllowHttp() && "http".equals(scheme))) {
                throw new DomainException("OUTBOUND_HTTPS_REQUIRED", "Outbound integrations must use HTTPS");
            }
            String host = uri.getHost().toLowerCase();
            if (!properties.getAllowedHosts().isEmpty() && !isAllowedHost(host)) {
                throw new DomainException("OUTBOUND_HOST_NOT_ALLOWED", "Outbound host is not included in the configured allowlist");
            }
            if (!properties.isAllowPrivateNetworks()) {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                boolean unsafe = Arrays.stream(addresses).anyMatch(this::isPrivateOrLocal);
                if (unsafe) {
                    throw new DomainException("OUTBOUND_PRIVATE_ADDRESS_DENIED", "Outbound URL resolves to a private or local address");
                }
            }
            return uri;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid();
        }
    }

    private boolean isAllowedHost(String host) {
        for (String configuredHost : properties.getAllowedHosts()) {
            if (configuredHost != null && host.equals(configuredHost.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPrivateOrLocal(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private DomainException invalid() {
        return new DomainException("INVALID_OUTBOUND_URL", "Outbound URL is invalid or unsafe");
    }
}

package com.corvian.payroll_payment_orchestrator.shared.outbound;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.outbound")
public class OutboundUrlProperties {
    private boolean allowHttp;
    private boolean allowPrivateNetworks;
    private List<String> allowedHosts = new ArrayList<>();

    public boolean isAllowHttp() { return allowHttp; }
    public void setAllowHttp(boolean allowHttp) { this.allowHttp = allowHttp; }
    public boolean isAllowPrivateNetworks() { return allowPrivateNetworks; }
    public void setAllowPrivateNetworks(boolean allowPrivateNetworks) { this.allowPrivateNetworks = allowPrivateNetworks; }
    public List<String> getAllowedHosts() { return allowedHosts; }
    public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
}

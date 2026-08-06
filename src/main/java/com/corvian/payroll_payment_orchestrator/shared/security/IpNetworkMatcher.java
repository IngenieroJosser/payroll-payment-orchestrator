package com.corvian.payroll_payment_orchestrator.shared.security;

import java.net.InetAddress;
import java.util.Collection;

public final class IpNetworkMatcher {
    private IpNetworkMatcher() {}

    public static boolean matchesAny(String candidate, Collection<String> rules) {
        if (candidate == null || candidate.isBlank() || rules == null) return false;
        for (String rule : rules) {
            if (matches(candidate, rule)) return true;
        }
        return false;
    }

    public static boolean isValidRule(String rule) {
        try {
            parse(rule);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static boolean isLiteralAddress(String value) {
        try {
            if (value == null || value.isBlank() || value.contains("%")) return false;
            if (!value.matches("^[0-9A-Fa-f:.]+$")) return false;
            InetAddress.getByName(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean matches(String candidate, String rule) {
        try {
            InetAddress candidateAddress = literal(candidate);
            Network network = parse(rule);
            byte[] candidateBytes = candidateAddress.getAddress();
            byte[] networkBytes = network.address().getAddress();
            if (candidateBytes.length != networkBytes.length) return false;
            int wholeBytes = network.prefixLength() / 8;
            int remainingBits = network.prefixLength() % 8;
            for (int index = 0; index < wholeBytes; index++) {
                if (candidateBytes[index] != networkBytes[index]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (candidateBytes[wholeBytes] & mask) == (networkBytes[wholeBytes] & mask);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static Network parse(String rule) {
        if (rule == null || rule.isBlank()) throw new IllegalArgumentException("IP rule is blank");
        String normalized = rule.trim();
        String[] parts = normalized.split("/", -1);
        if (parts.length > 2) throw new IllegalArgumentException("Invalid CIDR");
        InetAddress address = literal(parts[0]);
        int maximum = address.getAddress().length * 8;
        int prefix = parts.length == 1 ? maximum : Integer.parseInt(parts[1]);
        if (prefix < 0 || prefix > maximum) throw new IllegalArgumentException("Invalid CIDR prefix");
        return new Network(address, prefix);
    }

    private static InetAddress literal(String value) {
        if (!isLiteralAddress(value)) throw new IllegalArgumentException("Address must be an IP literal");
        try {
            return InetAddress.getByName(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid IP address", ex);
        }
    }

    private record Network(InetAddress address, int prefixLength) {}
}

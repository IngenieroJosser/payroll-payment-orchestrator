package com.corvian.payroll_payment_orchestrator.shared.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpNetworkMatcherTest {

    @Test
    void matchesExactAndCidrAddresses() {
        assertTrue(IpNetworkMatcher.matchesAny("10.20.30.40", List.of("10.0.0.0/8")));
        assertTrue(IpNetworkMatcher.matchesAny("192.168.1.10", List.of("192.168.1.10")));
        assertTrue(IpNetworkMatcher.matchesAny("2001:db8::10", List.of("2001:db8::/32")));
        assertFalse(IpNetworkMatcher.matchesAny("172.16.0.1", List.of("10.0.0.0/8")));
    }

    @Test
    void rejectsHostnamesAndMalformedRules() {
        assertFalse(IpNetworkMatcher.isValidRule("localhost"));
        assertFalse(IpNetworkMatcher.isValidRule("10.0.0.0/99"));
        assertFalse(IpNetworkMatcher.isLiteralAddress("example.com"));
    }
}

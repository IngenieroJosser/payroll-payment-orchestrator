package com.corvian.payroll_payment_orchestrator.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    @Test
    void shouldIssueAndValidateJwt() {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwtSecret("0123456789ABCDEF0123456789ABCDEF");
        JwtService service = new JwtService(properties, new ObjectMapper());

        String token = service.issueToken("subject-1", "user", List.of("payroll:read"));
        JwtService.JwtPrincipal principal = service.parseAndValidate(token);

        assertThat(principal.subject()).isEqualTo("subject-1");
        assertThat(principal.authorities()).contains("payroll:read");
    }
}

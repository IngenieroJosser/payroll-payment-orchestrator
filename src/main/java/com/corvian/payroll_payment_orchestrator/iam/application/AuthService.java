package com.corvian.payroll_payment_orchestrator.iam.application;

import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import com.corvian.payroll_payment_orchestrator.iam.domain.UserStatus;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.ApiClientEntity;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.JpaApiClientRepository;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.JpaUserRepository;
import com.corvian.payroll_payment_orchestrator.shared.exception.DomainException;
import com.corvian.payroll_payment_orchestrator.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {
    private final JpaUserRepository userRepository;
    private final JpaApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(JpaUserRepository userRepository, JpaApiClientRepository apiClientRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResult login(String email, String password) {
        var user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", "Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", "Invalid credentials");
        }
        List<String> authorities = AuthorityService.authorities(user);
        return new TokenResult(jwtService.issueToken(user.getId().toString(), "user", authorities), authorities);
    }

    @Transactional
    public TokenResult clientCredentials(String grantType, String clientId, String clientSecret) {
        if (!"client_credentials".equals(grantType)) {
            throw new DomainException("UNSUPPORTED_GRANT_TYPE", "Only client_credentials is supported");
        }
        ApiClientEntity client = apiClientRepository.findByClientId(clientId)
                .orElseThrow(() -> new DomainException("INVALID_CLIENT", "Invalid API client"));
        if (client.getStatus() != ApiClientStatus.ACTIVE || !passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {
            throw new DomainException("INVALID_CLIENT", "Invalid API client");
        }
        client.setLastUsedAt(OffsetDateTime.now());
        apiClientRepository.save(client);
        List<String> scopes = Arrays.stream(client.getScopes().split(" ")).filter(scope -> !scope.isBlank()).toList();
        return new TokenResult(jwtService.issueToken(client.getClientId(), "client", scopes), scopes);
    }

    public record TokenResult(String token, List<String> authorities) {}
}

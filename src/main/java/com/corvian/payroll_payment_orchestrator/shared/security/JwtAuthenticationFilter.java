package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.JpaApiClientRepository;
import com.corvian.payroll_payment_orchestrator.shared.security.context.ActorType;
import com.corvian.payroll_payment_orchestrator.shared.security.context.AuthenticatedActor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JpaApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtAuthenticationFilter(JwtService jwtService, JpaApiClientRepository apiClientRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String apiKeyHeader = request.getHeader("X-API-KEY");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authenticateJwt(authorization.substring(7).trim());
            return;
        }
        String apiKey = null;
        if (authorization != null && authorization.startsWith("ApiKey ")) apiKey = authorization.substring(7).trim();
        if ((apiKey == null || apiKey.isBlank()) && apiKeyHeader != null) apiKey = apiKeyHeader.trim();
        if (apiKey != null && !apiKey.isBlank()) authenticateApiKey(apiKey);
    }

    private void authenticateJwt(String token) {
        try {
            JwtService.JwtPrincipal jwt = jwtService.parseAndValidate(token);
            ActorType actorType = "client".equals(jwt.type()) ? ActorType.API_CLIENT : ActorType.USER;
            AuthenticatedActor principal = new AuthenticatedActor(
                    jwt.subject(), actorType, jwt.tenantId(), jwt.companyId(), jwt.platformAdmin());
            setAuthentication(principal, jwt.authorities());
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateApiKey(String token) {
        try {
            String[] parts = token.contains(".") ? token.split("\\.", 2) : token.split(":", 2);
            if (parts.length != 2) return;
            apiClientRepository.findByClientId(parts[0]).ifPresent(client -> {
                if (client.getStatus() == ApiClientStatus.ACTIVE
                        && passwordEncoder.matches(parts[1], client.getClientSecretHash())) {
                    List<String> scopes = Arrays.stream(client.getScopes().split(" "))
                            .filter(scope -> !scope.isBlank())
                            .toList();
                    AuthenticatedActor principal = new AuthenticatedActor(
                            client.getClientId(), ActorType.API_CLIENT, client.getTenantId(), client.getCompanyId(), false);
                    setAuthentication(principal, scopes);
                }
            });
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(AuthenticatedActor principal, List<String> authorities) {
        var granted = authorities.stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, granted));
    }
}

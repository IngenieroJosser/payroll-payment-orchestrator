package com.corvian.payroll_payment_orchestrator.shared.security;

import com.corvian.payroll_payment_orchestrator.iam.domain.ApiClientStatus;
import com.corvian.payroll_payment_orchestrator.iam.infrastructure.JpaApiClientRepository;
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
        String authorization = request.getHeader("Authorization");
        String apiKeyHeader = request.getHeader("X-API-KEY");

        String token = null;
        boolean isJwt = false;
        boolean isApiKey = false;

        if (authorization != null) {
            if (authorization.startsWith("Bearer ")) {
                token = authorization.substring(7).trim();
                isJwt = true;
            } else if (authorization.startsWith("ApiKey ")) {
                token = authorization.substring(7).trim();
                isApiKey = true;
            }
        } else if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            token = apiKeyHeader.trim();
            isApiKey = true;
        }

        if (isJwt && token != null) {
            try {
                JwtService.JwtPrincipal principal = jwtService.parseAndValidate(token);
                var authorities = principal.authorities().stream().map(SimpleGrantedAuthority::new).toList();
                var authentication = new UsernamePasswordAuthenticationToken(principal.subject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        } else if (isApiKey && token != null) {
            try {
                String[] parts = token.contains(".") ? token.split("\\.", 2) : token.split(":", 2);
                if (parts.length == 2) {
                    String clientId = parts[0];
                    String clientSecret = parts[1];

                    var clientOpt = apiClientRepository.findByClientId(clientId);
                    if (clientOpt.isPresent()) {
                        var client = clientOpt.get();
                        if (client.getStatus() == ApiClientStatus.ACTIVE
                                && passwordEncoder.matches(clientSecret, client.getClientSecretHash())) {

                            List<String> scopes = Arrays.stream(client.getScopes().split(" "))
                                    .filter(scope -> !scope.isBlank())
                                    .toList();

                            var authorities = scopes.stream().map(SimpleGrantedAuthority::new).toList();
                            var authentication = new UsernamePasswordAuthenticationToken(client.getClientId(), null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

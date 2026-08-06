package com.corvian.payroll_payment_orchestrator.shared.config;

import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyFilter;
import com.corvian.payroll_payment_orchestrator.shared.outbound.OutboundUrlProperties;
import com.corvian.payroll_payment_orchestrator.payroll.config.PayrollProperties;
import com.corvian.payroll_payment_orchestrator.banks.governance.BankProviderGovernanceProperties;
import com.corvian.payroll_payment_orchestrator.shared.deployment.DeploymentProperties;
import com.corvian.payroll_payment_orchestrator.shared.security.IpAllowlistFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.JwtAuthenticationFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.MtlsFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.RateLimitFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityProperties.class, OutboundUrlProperties.class, PayrollProperties.class, BankProviderGovernanceProperties.class, DeploymentProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            IpAllowlistFilter ipAllowlistFilter,
            MtlsFilter mtlsFilter,
            IdempotencyFilter idempotencyFilter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication is required\"}}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"ACCESS_DENIED\",\"message\":\"The authenticated principal is not authorized for this operation\"}}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/oauth/token").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(ipAllowlistFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mtlsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(idempotencyFilter, JwtAuthenticationFilter.class)
                .build();
    }
}

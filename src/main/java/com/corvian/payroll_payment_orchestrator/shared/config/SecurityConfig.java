package com.corvian.payroll_payment_orchestrator.shared.config;

import com.corvian.payroll_payment_orchestrator.idempotency.infrastructure.IdempotencyFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.IpAllowlistFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.JwtAuthenticationFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.MtlsFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.RateLimitFilter;
import com.corvian.payroll_payment_orchestrator.shared.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/api/v1/health").permitAll()
                        .requestMatchers("/api/v1/auth/login", "/api/v1/oauth/token").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(ipAllowlistFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(mtlsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(idempotencyFilter, JwtAuthenticationFilter.class)
                .build();
    }
}

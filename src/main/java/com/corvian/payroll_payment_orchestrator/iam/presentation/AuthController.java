package com.corvian.payroll_payment_orchestrator.iam.presentation;

import com.corvian.payroll_payment_orchestrator.iam.application.AuthService;
import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/auth/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenResult result = authService.login(request.email(), request.password());
        return ApiResponse.ok(new AuthResponse("Bearer", result.token(), result.expiresInSeconds(), result.authorities()));
    }

    @PostMapping("/oauth/token")
    public ApiResponse<AuthResponse> clientCredentials(@Valid @RequestBody ClientCredentialsTokenRequest request) {
        AuthService.TokenResult result = authService.clientCredentials(request.grantType(), request.clientId(), request.clientSecret());
        return ApiResponse.ok(new AuthResponse("Bearer", result.token(), result.expiresInSeconds(), result.authorities()));
    }
}

package com.corvian.payroll_payment_orchestrator.webhooks.presentation;

import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import com.corvian.payroll_payment_orchestrator.webhooks.application.CreatedWebhookEndpoint;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookService;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.WebhookEndpointEntity;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/webhooks")
public class WebhookController {
    private final WebhookService service;
    public WebhookController(WebhookService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAuthority('webhook:manage')")
    public ResponseEntity<ApiResponse<WebhookEndpointResponse>> create(@PathVariable UUID companyId,
            @Valid @RequestBody CreateWebhookEndpointRequest request) {
        CreatedWebhookEndpoint created = service.createWithSecret(companyId, request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(created.endpoint(), created.signingSecret())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('webhook:manage')")
    public ApiResponse<List<WebhookEndpointResponse>> list(@PathVariable UUID companyId) {
        return ApiResponse.ok(service.findByCompanyId(companyId).stream().map(entity -> toResponse(entity, null)).toList());
    }

    @PostMapping("/{webhookId}/disable")
    @PreAuthorize("hasAuthority('webhook:manage')")
    public ApiResponse<Map<String,String>> disable(@PathVariable UUID companyId, @PathVariable UUID webhookId) {
        service.disable(companyId, webhookId);
        return ApiResponse.ok(Map.of("status", "DISABLED"));
    }

    private WebhookEndpointResponse toResponse(WebhookEndpointEntity entity, String secret) {
        return new WebhookEndpointResponse(entity.getId(), entity.getCompanyId(), entity.getUrl(), entity.getEnabled(),
                entity.getCreatedAt(), entity.getUpdatedAt(), secret);
    }
}

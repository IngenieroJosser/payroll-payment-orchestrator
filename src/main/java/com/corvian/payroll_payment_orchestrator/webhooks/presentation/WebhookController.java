package com.corvian.payroll_payment_orchestrator.webhooks.presentation;

import com.corvian.payroll_payment_orchestrator.shared.response.ApiResponse;
import com.corvian.payroll_payment_orchestrator.webhooks.application.WebhookService;
import com.corvian.payroll_payment_orchestrator.webhooks.infrastructure.WebhookEndpointEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/webhooks")
public class WebhookController {
    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookEndpointResponse>> create(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateWebhookEndpointRequest request
    ) {
        WebhookEndpointEntity created = service.create(companyId, request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(toResponse(created)));
    }

    @GetMapping
    public ApiResponse<List<WebhookEndpointResponse>> list(@PathVariable UUID companyId) {
        return ApiResponse.ok(service.findByCompanyId(companyId).stream().map(this::toResponse).toList());
    }

    @PostMapping("/{webhookId}/disable")
    public ApiResponse<Map<String, String>> disable(@PathVariable UUID companyId, @PathVariable UUID webhookId) {
        service.disable(webhookId);
        return ApiResponse.ok(Map.of("status", "DISABLED"));
    }

    private WebhookEndpointResponse toResponse(WebhookEndpointEntity entity) {
        return new WebhookEndpointResponse(entity.getId(), entity.getCompanyId(), entity.getUrl(), entity.getEnabled(), entity.getCreatedAt());
    }
}

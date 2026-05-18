package com.corvian.payroll_payment_orchestrator.banks.infrastructure;

import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchRequest;
import com.corvian.payroll_payment_orchestrator.banks.application.BankBatchResponse;
import com.corvian.payroll_payment_orchestrator.banks.application.BankPaymentProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.bank.provider", havingValue = "rest")
public class RestBankPaymentProvider implements BankPaymentProvider {
    private final RestClient restClient = RestClient.create();

    @Override
    public BankBatchResponse sendPayrollBatch(BankBatchRequest request) {
        String baseUrl = System.getenv().getOrDefault("BANK_PROVIDER_BASE_URL", "http://localhost:9090");
        return restClient.post()
                .uri(baseUrl + "/payroll-batches")
                .header("Authorization", "Bearer " + System.getenv().getOrDefault("BANK_PROVIDER_TOKEN", "dev-token"))
                .body(request)
                .retrieve()
                .body(BankBatchResponse.class);
    }

    @Override
    public BankBatchResponse getBatchStatus(String externalBatchId) {
        String baseUrl = System.getenv().getOrDefault("BANK_PROVIDER_BASE_URL", "http://localhost:9090");
        return restClient.get()
                .uri(baseUrl + "/payroll-batches/" + externalBatchId)
                .header("Authorization", "Bearer " + System.getenv().getOrDefault("BANK_PROVIDER_TOKEN", "dev-token"))
                .retrieve()
                .body(BankBatchResponse.class);
    }
}

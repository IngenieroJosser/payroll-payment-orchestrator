package com.corvian.payroll_payment_orchestrator.payroll.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.payroll")
public class PayrollProperties {
    private int maxPaymentsPerBatch = 10_000;
    private BigDecimal maxBatchAmount = new BigDecimal("99999999999999999.99");
    private boolean makerCheckerRequired = true;

    public int getMaxPaymentsPerBatch() { return maxPaymentsPerBatch; }
    public void setMaxPaymentsPerBatch(int maxPaymentsPerBatch) { this.maxPaymentsPerBatch = maxPaymentsPerBatch; }
    public BigDecimal getMaxBatchAmount() { return maxBatchAmount; }
    public void setMaxBatchAmount(BigDecimal maxBatchAmount) { this.maxBatchAmount = maxBatchAmount; }
    public boolean isMakerCheckerRequired() { return makerCheckerRequired; }
    public void setMakerCheckerRequired(boolean makerCheckerRequired) { this.makerCheckerRequired = makerCheckerRequired; }
}

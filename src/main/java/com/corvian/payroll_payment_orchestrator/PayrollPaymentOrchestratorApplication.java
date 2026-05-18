package com.corvian.payroll_payment_orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PayrollPaymentOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PayrollPaymentOrchestratorApplication.class, args);
	}

}

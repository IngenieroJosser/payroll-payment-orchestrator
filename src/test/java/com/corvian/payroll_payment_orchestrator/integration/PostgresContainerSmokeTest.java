package com.corvian.payroll_payment_orchestrator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@SuppressWarnings({"resource", "unused"})
class PostgresContainerSmokeTest {
    private static final int POSTGRES_PORT = 5432;
    private static final int RABBITMQ_AMQP_PORT = 5672;
    private static final String DATABASE_NAME = "payroll_payment_orchestrator";
    private static final String INFRASTRUCTURE_USERNAME = "payroll_user";
    private static final String INFRASTRUCTURE_PASSWORD = "payroll_password";

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withExposedPorts(POSTGRES_PORT)
            .withEnv("POSTGRES_DB", DATABASE_NAME)
            .withEnv("POSTGRES_USER", INFRASTRUCTURE_USERNAME)
            .withEnv("POSTGRES_PASSWORD", INFRASTRUCTURE_PASSWORD);

    @Container
    static final GenericContainer<?> RABBITMQ = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:4-management-alpine"))
            .withExposedPorts(RABBITMQ_AMQP_PORT)
            .withEnv("RABBITMQ_DEFAULT_USER", INFRASTRUCTURE_USERNAME)
            .withEnv("RABBITMQ_DEFAULT_PASS", INFRASTRUCTURE_PASSWORD);

    @DynamicPropertySource
    static void registerInfrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> String.format(
                "jdbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(),
                POSTGRES.getMappedPort(POSTGRES_PORT),
                DATABASE_NAME));
        registry.add("spring.datasource.username", () -> INFRASTRUCTURE_USERNAME);
        registry.add("spring.datasource.password", () -> INFRASTRUCTURE_PASSWORD);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(RABBITMQ_AMQP_PORT));
        registry.add("spring.rabbitmq.username", () -> INFRASTRUCTURE_USERNAME);
        registry.add("spring.rabbitmq.password", () -> INFRASTRUCTURE_PASSWORD);
    }

    @Test
    void contextLoadsWithAllMigrationsAndInfrastructureAdapters() {
        // Successful application-context startup is the assertion.
    }
}

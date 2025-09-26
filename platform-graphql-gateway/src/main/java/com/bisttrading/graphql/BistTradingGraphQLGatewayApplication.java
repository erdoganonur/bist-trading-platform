package com.bisttrading.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * BIST Trading Platform GraphQL Gateway Application
 *
 * This service provides a unified GraphQL interface over the microservices architecture,
 * enabling efficient data fetching and real-time subscriptions for trading operations.
 *
 * Features:
 * - Unified GraphQL API for all trading operations
 * - Real-time subscriptions for market data and order updates
 * - DataLoader pattern to prevent N+1 problems
 * - JWT authentication integration
 * - Turkish market compliance
 */
@SpringBootApplication(scanBasePackages = {
    "com.bisttrading.graphql",
    "com.bisttrading.infrastructure.security",
    "com.bisttrading.common"
})
@EnableFeignClients(basePackages = "com.bisttrading.graphql.client")
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
    "com.bisttrading.graphql.repository"
})
@EntityScan(basePackages = {
    "com.bisttrading.graphql.entity",
    "com.bisttrading.infrastructure.persistence.entity"
})
@EnableAsync
public class BistTradingGraphQLGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BistTradingGraphQLGatewayApplication.class, args);
    }
}
package com.bisttrading.broker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for Broker Integration Service.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "BIST Trading Platform - Broker Integration Service",
        version = "1.0.0",
        description = """
            Broker Integration API for BIST Trading Platform.

            This service provides:
            - AlgoLab broker integration
            - Order routing to external brokers
            - Real-time market data feed
            - WebSocket connections for live updates
            - Broker-specific authentication
            """,
        contact = @Contact(
            name = "BIST Trading Platform Team",
            email = "api-support@bisttrading.com.tr"
        ),
        license = @License(
            name = "Proprietary"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8084", description = "Local Development"),
        @Server(url = "http://localhost:8080", description = "API Gateway")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token. Obtain from /api/v1/auth/login"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("Broker Integration Service API")
                .version("1.0.0")
                .description("External broker integrations and trading"));
    }
}
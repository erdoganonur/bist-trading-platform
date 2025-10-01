package com.bisttrading.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for User Management Service.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "BIST Trading Platform - User Management Service",
        version = "1.0.0",
        description = """
            User Management API for BIST Trading Platform.

            This service provides:
            - User registration and authentication
            - JWT token management
            - User profile management
            - Role and permission management
            - Security features (2FA, password reset)
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
        @Server(url = "http://localhost:8081", description = "Local Development"),
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

}
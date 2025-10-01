package com.bisttrading.oms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for Order Management Service.
 *
 * Provides comprehensive API documentation with:
 * - Service information and version
 * - Security schemes (JWT Bearer)
 * - Server definitions for environments
 * - Common response schemas
 * - Example requests/responses
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "BIST Trading Platform - Order Management Service",
        version = "1.0.0",
        description = """
            Comprehensive Order Management API for BIST Trading Platform.

            This service provides complete order lifecycle management including:
            - Individual order operations (create, modify, cancel)
            - Batch order processing
            - Advanced order types (bracket, OCO, iceberg)
            - Order history and statistics
            - Real-time order updates
            - Risk management and validation

            All endpoints require authentication via JWT Bearer token.
            """,
        contact = @Contact(
            name = "BIST Trading Platform Team",
            email = "api-support@bisttrading.com.tr",
            url = "https://api.bisttrading.com.tr/docs"
        ),
        license = @License(
            name = "Proprietary",
            url = "https://bisttrading.com.tr/license"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8082",
            description = "Local Development Server"
        ),
        @Server(
            url = "https://api-dev.bisttrading.com.tr",
            description = "Development Environment"
        ),
        @Server(
            url = "https://api-staging.bisttrading.com.tr",
            description = "Staging Environment"
        ),
        @Server(
            url = "https://api.bisttrading.com.tr",
            description = "Production Environment"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Bearer token authentication. Obtain token from /api/v1/auth/login endpoint."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new io.swagger.v3.oas.models.info.Info()
                .title("Order Management Service API")
                .version("1.0.0")
                .description("BIST Trading Platform Order Management API"))

            // Add common response schemas
            .components(new io.swagger.v3.oas.models.Components()

                // Error Response Schema
                .addSchemas("ErrorResponse", new Schema<>()
                    .type("object")
                    .addProperty("timestamp", new Schema<>()
                        .type("string")
                        .format("date-time")
                        .example("2023-12-01T10:30:00"))
                    .addProperty("status", new Schema<>()
                        .type("integer")
                        .example(400))
                    .addProperty("error", new Schema<>()
                        .type("string")
                        .example("Bad Request"))
                    .addProperty("message", new Schema<>()
                        .type("string")
                        .example("Invalid request parameters"))
                    .addProperty("path", new Schema<>()
                        .type("string")
                        .example("/api/v1/orders"))
                    .addProperty("validationErrors", new Schema<>()
                        .type("object")
                        .additionalProperties(new Schema<>().type("string")))
                )

                // Order Status Enum Schema
                .addSchemas("OrderStatus", new Schema<>()
                    .type("string")
                    ._enum(java.util.List.of("NEW", "PARTIALLY_FILLED", "FILLED", "CANCELLED", "REJECTED", "EXPIRED"))
                )

                // Order Type Enum Schema
                .addSchemas("OrderType", new Schema<>()
                    .type("string")
                    ._enum(java.util.List.of("MARKET", "LIMIT", "STOP", "STOP_LIMIT"))
                )

                // Order Side Enum Schema
                .addSchemas("OrderSide", new Schema<>()
                    .type("string")
                    ._enum(java.util.List.of("BUY", "SELL"))
                )

                // Time In Force Enum Schema
                .addSchemas("TimeInForce", new Schema<>()
                    .type("string")
                    ._enum(java.util.List.of("DAY", "GTC", "IOC", "FOK"))
                )

                // Common API Responses
                .addResponses("400", new ApiResponse()
                    .description("Bad Request")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                            .addExamples("validation-error", new Example()
                                .summary("Validation Error")
                                .description("Request validation failed")
                                .value("""
                                    {
                                      "timestamp": "2023-12-01T10:30:00",
                                      "status": 400,
                                      "error": "Validation Failed",
                                      "message": "Request validation failed",
                                      "path": "/api/v1/orders",
                                      "validationErrors": {
                                        "quantity": "Quantity must be greater than 0",
                                        "symbol": "Symbol is required"
                                      }
                                    }
                                    """)))))

                .addResponses("401", new ApiResponse()
                    .description("Unauthorized")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))))

                .addResponses("403", new ApiResponse()
                    .description("Forbidden")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))))

                .addResponses("404", new ApiResponse()
                    .description("Not Found")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))))

                .addResponses("409", new ApiResponse()
                    .description("Conflict")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))))

                .addResponses("500", new ApiResponse()
                    .description("Internal Server Error")
                    .content(new Content()
                        .addMediaType("application/json", new MediaType()
                            .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))))
            );
    }
}
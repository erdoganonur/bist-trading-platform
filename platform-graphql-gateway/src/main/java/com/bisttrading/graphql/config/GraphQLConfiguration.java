package com.bisttrading.graphql.config;

import graphql.execution.instrumentation.Instrumentation;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GraphQL Configuration for BIST Trading Platform
 *
 * Configures GraphQL runtime, scalars, and performance optimizations
 */
@Configuration
public class GraphQLConfiguration {

    /**
     * Configure custom scalars for Turkish market requirements
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
            // Extended scalars for financial data
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.PositiveInt)
            .scalar(ExtendedScalars.NonNegativeInt)
            .scalar(ExtendedScalars.UUID)

            // Custom scalars for missing types
            .scalar(createBigIntegerScalar())
            .scalar(createUploadScalar())

            // Custom Turkish locale scalars
            .scalar(createDecimalScalar())
            .scalar(createTurkishDecimalScalar())
            .scalar(createTCKNScalar());
    }

    /**
     * DataLoader dispatcher for N+1 problem prevention
     */
    @Bean
    public Instrumentation dataLoaderDispatcherInstrumentation() {
        // Use default Spring GraphQL DataLoader dispatcher
        return new Instrumentation() {};
    }

    /**
     * Executor for async GraphQL operations
     */
    @Bean
    public Executor graphqlExecutor() {
        return Executors.newFixedThreadPool(20);
    }

    /**
     * Custom Decimal scalar for financial calculations
     */
    private GraphQLScalarType createDecimalScalar() {
        return GraphQLScalarType.newScalar()
            .name("Decimal")
            .description("Decimal type for precise financial calculations")
            .coercing(new DecimalCoercing())
            .build();
    }

    /**
     * Custom scalar for Turkish decimal formatting
     */
    private GraphQLScalarType createTurkishDecimalScalar() {
        return GraphQLScalarType.newScalar()
            .name("TurkishDecimal")
            .description("Decimal formatted for Turkish locale (comma as decimal separator)")
            .coercing(new TurkishDecimalCoercing())
            .build();
    }

    /**
     * Custom scalar for Turkish Identity Number validation
     */
    private GraphQLScalarType createTCKNScalar() {
        return GraphQLScalarType.newScalar()
            .name("TCKN")
            .description("Turkish Citizenship Number with validation")
            .coercing(new TCKNCoercing())
            .build();
    }

    /**
     * Custom BigInteger scalar
     */
    private GraphQLScalarType createBigIntegerScalar() {
        return ExtendedScalars.GraphQLBigInteger;
    }

    /**
     * Custom Upload scalar for file uploads
     */
    private GraphQLScalarType createUploadScalar() {
        return GraphQLScalarType.newScalar()
            .name("Upload")
            .description("File upload scalar")
            .coercing(new UploadCoercing())
            .build();
    }
}
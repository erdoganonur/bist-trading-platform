package com.bisttrading.graphql.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Feign clients used by GraphQL Gateway
 */
@Slf4j
@Configuration
public class ServiceClientConfiguration {

    /**
     * Configure request timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            10, TimeUnit.SECONDS, // connect timeout
            30, TimeUnit.SECONDS, // read timeout
            true // follow redirects
        );
    }

    /**
     * Configure retry policy
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100, // initial retry interval
            1000, // max retry interval
            3 // max attempts
        );
    }

    /**
     * Configure logging level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Custom error decoder for service errors
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new GraphQLServiceErrorDecoder();
    }

    /**
     * Custom error decoder that converts service errors to GraphQL errors
     */
    public static class GraphQLServiceErrorDecoder implements ErrorDecoder {

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            switch (response.status()) {
                case 400:
                    return new RuntimeException("Bad request to service: " + methodKey);
                case 401:
                    return new RuntimeException("Unauthorized service call: " + methodKey);
                case 403:
                    return new RuntimeException("Forbidden service call: " + methodKey);
                case 404:
                    return new RuntimeException("Service resource not found: " + methodKey);
                case 500:
                    return new RuntimeException("Service internal error: " + methodKey);
                case 503:
                    return new RuntimeException("Service unavailable: " + methodKey);
                default:
                    return new RuntimeException("Service error " + response.status() + ": " + methodKey);
            }
        }
    }
}
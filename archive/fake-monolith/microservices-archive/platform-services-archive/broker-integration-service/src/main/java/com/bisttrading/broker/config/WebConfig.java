package com.bisttrading.broker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Web configuration for HTTP clients and REST templates
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final AlgoLabProperties algoLabProperties;

    /**
     * RestTemplate bean for HTTP client operations
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Configure timeouts from AlgoLab properties
        factory.setConnectTimeout((int) algoLabProperties.getTimeout().getConnect().toMillis());
        factory.setReadTimeout((int) algoLabProperties.getTimeout().getRead().toMillis());

        return builder
                .setConnectTimeout(algoLabProperties.getTimeout().getConnect())
                .setReadTimeout(algoLabProperties.getTimeout().getRead())
                .requestFactory(() -> factory)
                .build();
    }

    /**
     * ObjectMapper bean for JSON serialization/deserialization
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
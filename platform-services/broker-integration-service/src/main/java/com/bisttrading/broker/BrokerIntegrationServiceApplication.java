package com.bisttrading.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Broker Integration Service main application class
 * Handles integration with AlgoLab broker API
 */
@SpringBootApplication
@EnableAsync
public class BrokerIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerIntegrationServiceApplication.class, args);
    }
}
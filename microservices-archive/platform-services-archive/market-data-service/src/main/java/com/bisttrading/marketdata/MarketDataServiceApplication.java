package com.bisttrading.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Market Data Service main application class
 * Handles real-time market data processing and persistence to TimescaleDB
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
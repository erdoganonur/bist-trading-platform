package com.bisttrading.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Order Management Service Application
 */
@SpringBootApplication(scanBasePackages = {
    "com.bisttrading.oms",
    "com.bisttrading.infrastructure.persistence"
})
@EntityScan(basePackages = {
    "com.bisttrading.oms.model",
    "com.bisttrading.infrastructure.persistence.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.bisttrading.oms.repository",
    "com.bisttrading.infrastructure.persistence.repository"
})
public class OrderManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagementServiceApplication.class, args);
    }
}
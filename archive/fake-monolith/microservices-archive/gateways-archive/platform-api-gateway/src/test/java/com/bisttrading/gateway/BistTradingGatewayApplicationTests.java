package com.bisttrading.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for BIST Trading Gateway Application.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.cloud.gateway.routes[0].id=test-route",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**"
})
class BistTradingGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully
    }
}
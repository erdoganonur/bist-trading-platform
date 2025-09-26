package com.bisttrading.oms.controller;

import com.bisttrading.oms.dto.OrderRequest;
import com.bisttrading.oms.dto.OrderResponse;
import com.bisttrading.oms.model.OMSOrder;
import com.bisttrading.oms.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController.
 *
 * Tests the complete request-response cycle including:
 * - Authentication and authorization
 * - Request validation
 * - Business logic execution
 * - Response formatting
 * - Error handling
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private OrderRequest validOrderRequest;
    private OMSOrder testOrder;

    @BeforeEach
    void setUp() {
        // Clean up database
        orderRepository.deleteAll();

        // Prepare test data
        validOrderRequest = OrderRequest.builder()
            .symbol("THYAO")
            .side(OMSOrder.OrderSide.BUY)
            .type(OMSOrder.OrderType.LIMIT)
            .quantity(BigDecimal.valueOf(1000))
            .price(BigDecimal.valueOf(45.75))
            .timeInForce(OMSOrder.TimeInForce.GTC)
            .accountId("ACC-123")
            .notes("Integration test order")
            .build();

        testOrder = OMSOrder.builder()
            .orderId("TEST-ORDER-001")
            .clientOrderId("CLIENT-001")
            .userId("testuser")
            .symbol("THYAO")
            .side(OMSOrder.OrderSide.BUY)
            .type(OMSOrder.OrderType.LIMIT)
            .quantity(BigDecimal.valueOf(1000))
            .price(BigDecimal.valueOf(45.75))
            .status(OMSOrder.OrderStatus.NEW)
            .timeInForce(OMSOrder.TimeInForce.GTC)
            .filledQuantity(BigDecimal.ZERO)
            .remainingQuantity(BigDecimal.valueOf(1000))
            .commission(BigDecimal.ZERO)
            .accountId("ACC-123")
            .active(true)
            .build();
    }

    // =====================================
    // QUERY ENDPOINT TESTS
    // =====================================

    @Test
    @DisplayName("GET /api/v1/orders - Should return user orders with pagination")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrders_WhenValidRequest_ShouldReturnPagedOrders() throws Exception {
        // Given: Save test order
        orderRepository.save(testOrder);

        // When & Then
        mockMvc.perform(get("/api/v1/orders")
                .param("size", "20")
                .param("page", "0")
                .param("sort", "createdAt,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId", is("TEST-ORDER-001")))
                .andExpect(jsonPath("$.content[0].symbol", is("THYAO")))
                .andExpected(jsonPath("$.page.size", is(20)))
                .andExpected(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/orders - Should filter orders by status")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrders_WithStatusFilter_ShouldReturnFilteredOrders() throws Exception {
        // Given: Save orders with different statuses
        orderRepository.save(testOrder);

        OMSOrder filledOrder = testOrder.toBuilder()
            .orderId("TEST-ORDER-002")
            .status(OMSOrder.OrderStatus.FILLED)
            .filledQuantity(BigDecimal.valueOf(1000))
            .remainingQuantity(BigDecimal.ZERO)
            .build();
        orderRepository.save(filledOrder);

        // When & Then: Filter by NEW status
        mockMvc.perform(get("/api/v1/orders")
                .param("status", "NEW")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content", hasSize(1)))
                .andExpected(jsonPath("$.content[0].status", is("NEW")));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should return specific order")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrder_WhenOrderExists_ShouldReturnOrder() throws Exception {
        // Given
        orderRepository.save(testOrder);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/{orderId}", "TEST-ORDER-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.orderId", is("TEST-ORDER-001")))
                .andExpected(jsonPath("$.symbol", is("THYAO")))
                .andExpected(jsonPath("$.status", is("NEW")))
                .andExpected(jsonPath("$._links").exists())
                .andExpected(jsonPath("$._links.self").exists());
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should return 404 when order not found")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrder_WhenOrderNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", "NON-EXISTENT")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isNotFound())
                .andExpected(jsonPath("$.error", is("Order Not Found")))
                .andExpected(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/active - Should return only active orders")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getActiveOrders_ShouldReturnOnlyActiveOrders() throws Exception {
        // Given: Active and inactive orders
        orderRepository.save(testOrder); // Active

        OMSOrder cancelledOrder = testOrder.toBuilder()
            .orderId("TEST-ORDER-002")
            .status(OMSOrder.OrderStatus.CANCELLED)
            .active(false)
            .build();
        orderRepository.save(cancelledOrder);

        // When & Then
        mockMvc.perform(get("/api/v1/orders/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$", hasSize(1)))
                .andExpected(jsonPath("$[0].status", is("NEW")))
                .andExpected(jsonPath("$[0].active", is(true)));
    }

    // =====================================
    // COMMAND ENDPOINT TESTS
    // =====================================

    @Test
    @DisplayName("POST /api/v1/orders - Should create new order successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void placeOrder_WhenValidRequest_ShouldCreateOrder() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andDo(print())
                .andExpected(status().isCreated())
                .andExpected(jsonPath("$.symbol", is("THYAO")))
                .andExpected(jsonPath("$.side", is("BUY")))
                .andExpected(jsonPath("$.type", is("LIMIT")))
                .andExpected(jsonPath("$.quantity", is(1000.0)))
                .andExpected(jsonPath("$.price", is(45.75)))
                .andExpected(jsonPath("$.status", is("NEW")))
                .andExpected(jsonPath("$._links.self").exists())
                .andExpected(jsonPath("$._links.cancel").exists())
                .andExpected(jsonPath("$._links.modify").exists());
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should validate required fields")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void placeOrder_WhenMissingRequiredFields_ShouldReturn400() throws Exception {
        // Given: Invalid request (missing symbol)
        OrderRequest invalidRequest = OrderRequest.builder()
            .side(OMSOrder.OrderSide.BUY)
            .type(OMSOrder.OrderType.LIMIT)
            .quantity(BigDecimal.valueOf(1000))
            .timeInForce(OMSOrder.TimeInForce.GTC)
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", is("Validation Failed")))
                .andExpected(jsonPath("$.validationErrors.symbol", is("Symbol is required")));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Should validate price for limit orders")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void placeOrder_WhenLimitOrderWithoutPrice_ShouldReturn400() throws Exception {
        // Given: Limit order without price
        OrderRequest invalidRequest = validOrderRequest.toBuilder()
            .price(null)
            .build();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("DELETE /api/v1/orders/{orderId} - Should cancel order successfully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void cancelOrder_WhenOrderCanBeCancelled_ShouldCancelOrder() throws Exception {
        // Given: Active order
        orderRepository.save(testOrder);

        // When & Then
        mockMvc.perform(delete("/api/v1/orders/{orderId}", "TEST-ORDER-001")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.orderId", is("TEST-ORDER-001")))
                .andExpected(jsonPath("$.status", is("CANCELLED")))
                .andExpected(jsonPath("$.cancelledAt").exists())
                .andExpected(jsonPath("$._links.self").exists());
    }

    // =====================================
    // SECURITY TESTS
    // =====================================

    @Test
    @DisplayName("Should require authentication for all endpoints")
    void allEndpoints_WithoutAuthentication_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpected(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/orders/123"))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should require USER role for order operations")
    @WithMockUser(username = "testuser", roles = {"GUEST"})
    void orderEndpoints_WithoutUserRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpected(status().isForbidden());

        mockMvc.perform(post("/api/v1/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest)))
                .andExpected(status().isForbidden());
    }

    // =====================================
    // ERROR HANDLING TESTS
    // =====================================

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void placeOrder_WithMalformedJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", is("Malformed Request")));
    }

    @Test
    @DisplayName("Should handle type mismatch gracefully")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrders_WithInvalidStatusEnum_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .param("status", "INVALID_STATUS")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", is("Type Mismatch")));
    }

    // =====================================
    // PAGINATION TESTS
    // =====================================

    @Test
    @DisplayName("Should handle pagination correctly")
    @WithMockUser(username = "testuser", roles = {"USER"})
    void getOrders_WithPagination_ShouldReturnCorrectPage() throws Exception {
        // Given: Multiple orders
        for (int i = 0; i < 25; i++) {
            OMSOrder order = testOrder.toBuilder()
                .orderId("TEST-ORDER-" + String.format("%03d", i))
                .clientOrderId("CLIENT-" + String.format("%03d", i))
                .build();
            orderRepository.save(order);
        }

        // When & Then: Test second page with size 10
        mockMvc.perform(get("/api/v1/orders")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.content", hasSize(10)))
                .andExpected(jsonPath("$.page.number", is(1)))
                .andExpected(jsonPath("$.page.size", is(10)))
                .andExpected(jsonPath("$.page.totalElements", is(25)))
                .andExpected(jsonPath("$.page.totalPages", is(3)));
    }
}
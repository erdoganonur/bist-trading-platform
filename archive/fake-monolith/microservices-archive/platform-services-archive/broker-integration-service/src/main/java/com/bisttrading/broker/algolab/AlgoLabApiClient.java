package com.bisttrading.broker.algolab;

import com.bisttrading.broker.algolab.model.*;
import com.bisttrading.broker.algolab.model.response.CancelOrderResponse;
import com.bisttrading.broker.algolab.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * AlgoLab API Client - Java port of the Python client
 * Provides comprehensive trading API access for BIST markets
 */
@Component
public class AlgoLabApiClient {

    private static final Logger logger = LoggerFactory.getLogger(AlgoLabApiClient.class);

    private static final String DEFAULT_BASE_URL = "https://api.algolab.com.tr";
    private static final String API_VERSION = "v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    // SessionManager removed - using direct token management

    // Configuration
    private final String baseUrl;
    private final int connectionTimeout;
    private final int readTimeout;
    private final int maxRetries;

    // Session state
    private final Map<String, String> sessionHeaders = new ConcurrentHashMap<>();
    private volatile boolean authenticated = false;
    private volatile LocalDateTime lastHeartbeat;

    public AlgoLabApiClient(RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        // Configuration - using default values for now
        this.baseUrl = "https://api.algolab.com.tr";
        this.connectionTimeout = 30000;
        this.readTimeout = 60000;
        this.maxRetries = 3;

        setupRestTemplate();
    }

    /**
     * Authenticate with AlgoLab API
     */
    public AuthenticationResponse authenticate(String username, String password) {
        logger.info("Authenticating with AlgoLab API for user: {}", username);

        try {
            AuthenticationRequest request = AuthenticationRequest.builder()
                .username(username)
                .password(password)
                .build();

            String endpoint = buildUrl("/auth/login");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AuthenticationRequest> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<AuthenticationResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                httpEntity,
                AuthenticationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AuthenticationResponse authResponse = response.getBody();

                // Store session information
                sessionHeaders.put("Authorization", "Bearer " + authResponse.getToken());
                sessionHeaders.put("X-Session-ID", authResponse.getSessionId());
                authenticated = true;
                lastHeartbeat = LocalDateTime.now();

                // Session saved in memory - consider persistence in production

                logger.info("Authentication successful. Session ID: {}", authResponse.getSessionId());
                return authResponse;

            } else {
                throw new AlgoLabAuthenticationException("Authentication failed: Invalid response");
            }

        } catch (Exception e) {
            logger.error("Authentication failed", e);
            authenticated = false;
            sessionHeaders.clear();
            throw AlgoLabErrorHandler.handleException(e);
        }
    }

    /**
     * Get account information
     */
    public AccountInfo getAccountInfo() {
        ensureAuthenticated();

        try {
            String endpoint = buildUrl("/account/info");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<AccountInfo> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                httpEntity,
                AccountInfo.class
            );

            logger.debug("Retrieved account info successfully");
            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get account info", e);
            throw new AlgoLabException("Failed to get account info: " + e.getMessage(), e);
        }
    }

    /**
     * Get portfolio positions
     */
    public List<Position> getPositions() {
        ensureAuthenticated();

        try {
            String endpoint = buildUrl("/portfolio/positions");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<PositionResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                httpEntity,
                PositionResponse.class
            );

            logger.debug("Retrieved {} positions", response.getBody().getPositions().size());
            return response.getBody().getPositions();

        } catch (Exception e) {
            logger.error("Failed to get positions", e);
            throw new AlgoLabException("Failed to get positions: " + e.getMessage(), e);
        }
    }

    /**
     * Place a new order
     */
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        AlgoLabErrorHandler.validateAuthentication(authenticated);
        AlgoLabErrorHandler.validateOrderRequest(orderRequest);

        try {
            logger.info("Placing order: {} {} {} at {}",
                       orderRequest.getSide(),
                       orderRequest.getQuantity(),
                       orderRequest.getSymbol(),
                       orderRequest.getPrice());

            String endpoint = buildUrl("/orders/place");

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<OrderResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                httpEntity,
                OrderResponse.class
            );

            OrderResponse orderResponse = response.getBody();
            logger.info("Order placed successfully. Order ID: {}", orderResponse.getOrderId());

            return orderResponse;

        } catch (HttpClientErrorException e) {
            logger.error("Order placement failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AlgoLabException("Order placement failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            logger.error("Order placement failed with unexpected error", e);
            throw new AlgoLabException("Order placement failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel an existing order
     */
    public CancelOrderResponse cancelOrder(String orderId) {
        ensureAuthenticated();

        try {
            logger.info("Cancelling order: {}", orderId);

            String endpoint = buildUrl("/orders/" + orderId + "/cancel");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<CancelOrderResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.DELETE,
                httpEntity,
                CancelOrderResponse.class
            );

            CancelOrderResponse cancelResponse = response.getBody();
            logger.info("Order cancelled successfully: {}", orderId);

            return cancelResponse;

        } catch (Exception e) {
            logger.error("Order cancellation failed for order: {}", orderId, e);
            throw new AlgoLabException("Order cancellation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get order status
     */
    public OrderStatus getOrderStatus(String orderId) {
        ensureAuthenticated();

        try {
            String endpoint = buildUrl("/orders/" + orderId);

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<OrderStatus> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                httpEntity,
                OrderStatus.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get order status for order: {}", orderId, e);
            throw new AlgoLabException("Failed to get order status: " + e.getMessage(), e);
        }
    }

    /**
     * Get active orders
     */
    public List<OrderResponse> getActiveOrders() {
        ensureAuthenticated();

        try {
            String endpoint = buildUrl("/orders/active");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<OrderListResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                httpEntity,
                OrderListResponse.class
            );

            return response.getBody().getOrders();

        } catch (Exception e) {
            logger.error("Failed to get active orders", e);
            throw new AlgoLabException("Failed to get active orders: " + e.getMessage(), e);
        }
    }

    /**
     * Get market data for a symbol
     */
    public MarketData getMarketData(String symbol) {
        AlgoLabErrorHandler.validateAuthentication(authenticated);
        AlgoLabErrorHandler.validateSymbol(symbol);

        try {
            String endpoint = buildUrl("/market/data/" + symbol);

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<MarketData> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                httpEntity,
                MarketData.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get market data for symbol: {}", symbol, e);
            throw AlgoLabErrorHandler.handleException(e);
        }
    }

    /**
     * Get order book for a symbol
     */
    public OrderBook getOrderBook(String symbol, int depth) {
        ensureAuthenticated();

        try {
            String endpoint = buildUrl("/market/orderbook/" + symbol);

            URI uri = UriComponentsBuilder.fromHttpUrl(endpoint)
                .queryParam("depth", depth)
                .build()
                .toUri();

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<OrderBook> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                OrderBook.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to get order book for symbol: {}", symbol, e);
            throw new AlgoLabException("Failed to get order book: " + e.getMessage(), e);
        }
    }

    /**
     * Send heartbeat to maintain session
     */
    public void sendHeartbeat() {
        if (!authenticated) {
            return;
        }

        try {
            String endpoint = buildUrl("/session/heartbeat");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<?> rawResponse = restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                httpEntity,
                Map.class
            );
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) rawResponse;

            if (response.getStatusCode() == HttpStatus.OK) {
                lastHeartbeat = LocalDateTime.now();
                logger.debug("Heartbeat sent successfully");
            }

        } catch (Exception e) {
            logger.warn("Heartbeat failed, may need to re-authenticate", e);
            authenticated = false;
        }
    }

    /**
     * Send heartbeat asynchronously
     */
    public CompletableFuture<Map<String, Object>> heartbeat() {
        if (!authenticated) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = buildUrl("/session/heartbeat");
                HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

                ResponseEntity<?> rawResponse = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class
                );
                @SuppressWarnings("unchecked")
                ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) rawResponse;

                if (response.getStatusCode() == HttpStatus.OK) {
                    lastHeartbeat = LocalDateTime.now();
                    logger.debug("Heartbeat sent successfully");
                    return response.getBody();
                }
                return null;

            } catch (Exception e) {
                logger.warn("Heartbeat failed", e);
                throw new AlgoLabException("Heartbeat failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Refresh authentication token
     */
    public CompletableFuture<AuthenticationResponse> refreshToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = buildUrl("/auth/refresh");

                Map<String, String> refreshRequest = Map.of("refresh_token", token);
                HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(refreshRequest, createAuthHeaders());

                ResponseEntity<AuthenticationResponse> response = restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    httpEntity,
                    AuthenticationResponse.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    AuthenticationResponse authResponse = response.getBody();
                    if (authResponse != null && authResponse.getAccessToken() != null) {
                        // Update session headers with new token
                        sessionHeaders.put("Authorization", "Bearer " + authResponse.getAccessToken());
                        logger.info("Token refreshed successfully");
                        return authResponse;
                    }
                }

                logger.error("Token refresh failed - no valid response");
                return null;

            } catch (Exception e) {
                logger.error("Token refresh failed", e);
                throw new AlgoLabException("Token refresh failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Logout and invalidate session
     */
    public void logout() {
        if (!authenticated) {
            return;
        }

        try {
            String endpoint = buildUrl("/auth/logout");

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            restTemplate.exchange(
                endpoint,
                HttpMethod.POST,
                httpEntity,
                Void.class
            );

            logger.info("Logged out successfully");

        } catch (Exception e) {
            logger.warn("Logout request failed, clearing session anyway", e);
        } finally {
            // Clear session regardless of logout success
            sessionHeaders.clear();
            authenticated = false;
        }
    }

    /**
     * Logout asynchronously
     */
    public CompletableFuture<Void> logout(String token) {
        if (!authenticated) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String endpoint = buildUrl("/auth/logout");
                HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

                restTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class
                );

                logger.info("Logged out successfully");

            } catch (Exception e) {
                logger.warn("Logout request failed", e);
            } finally {
                // Clear session regardless of logout success
                sessionHeaders.clear();
                authenticated = false;
            }
        });
    }

    /**
     * Check if client is authenticated
     */
    public boolean isAuthenticated() {
        return authenticated && sessionHeaders.containsKey("Authorization");
    }

    // Private helper methods

    private void setupRestTemplate() {
        // RestTemplate is already configured via RestTemplateBuilder
        // Additional configuration can be added here if needed
    }

    private String buildUrl(String endpoint) {
        return baseUrl + "/" + API_VERSION + endpoint;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        sessionHeaders.forEach(headers::set);

        return headers;
    }

    private void ensureAuthenticated() {
        if (!authenticated) {
            throw new AlgoLabException("Not authenticated. Please call authenticate() first.");
        }

        // Check if session is still valid (optional heartbeat check)
        if (lastHeartbeat != null && lastHeartbeat.isBefore(LocalDateTime.now().minusMinutes(5))) {
            logger.debug("Session may be stale, sending heartbeat");
            sendHeartbeat();
        }
    }


    /**
     * Modify an existing order
     */
    public OrderResponse modifyOrder(String orderId, OrderModificationRequest modificationRequest) {
        AlgoLabErrorHandler.validateAuthentication(authenticated);

        if (orderId == null || orderId.trim().isEmpty()) {
            throw new AlgoLabValidationException("Order ID is required");
        }

        try {
            modificationRequest.validate();

            String endpoint = buildUrl("/orders/" + orderId + "/modify");

            HttpEntity<OrderModificationRequest> httpEntity = new HttpEntity<>(modificationRequest, createAuthHeaders());

            ResponseEntity<OrderResponse> response = restTemplate.exchange(
                endpoint,
                HttpMethod.PUT,
                httpEntity,
                OrderResponse.class
            );

            logger.info("Order modified successfully: {}", orderId);
            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to modify order: {}", orderId, e);
            throw AlgoLabErrorHandler.handleException(e);
        }
    }

    /**
     * Get orders by status filter
     */
    public List<OrderResponse> getOrders(String userId, String statusFilter) {
        AlgoLabErrorHandler.validateAuthentication(authenticated);

        try {
            String endpoint = buildUrl("/orders");

            URI uri = UriComponentsBuilder.fromHttpUrl(endpoint)
                .queryParam("user_id", userId)
                .queryParam("status", statusFilter)
                .build()
                .toUri();

            HttpEntity<?> httpEntity = new HttpEntity<>(createAuthHeaders());

            ResponseEntity<OrderListResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                httpEntity,
                OrderListResponse.class
            );

            return response.getBody().getOrders();

        } catch (Exception e) {
            logger.error("Failed to get orders for user: {} status: {}", userId, statusFilter, e);
            throw AlgoLabErrorHandler.handleException(e);
        }
    }


    // Convenience methods for common order types

    /**
     * Place a market buy order
     */
    public OrderResponse marketBuy(String symbol, int quantity) {
        OrderRequest request = OrderRequest.builder()
            .symbol(symbol)
            .side(OrderSide.BUY)
            .type(OrderType.MARKET)
            .quantity(quantity)
            .build();

        return placeOrder(request);
    }

    /**
     * Place a market sell order
     */
    public OrderResponse marketSell(String symbol, int quantity) {
        OrderRequest request = OrderRequest.builder()
            .symbol(symbol)
            .side(OrderSide.SELL)
            .type(OrderType.MARKET)
            .quantity(quantity)
            .build();

        return placeOrder(request);
    }

    /**
     * Place a limit buy order
     */
    public OrderResponse limitBuy(String symbol, int quantity, BigDecimal price) {
        OrderRequest request = OrderRequest.builder()
            .symbol(symbol)
            .side(OrderSide.BUY)
            .type(OrderType.LIMIT)
            .quantity(quantity)
            .price(price)
            .build();

        return placeOrder(request);
    }

    /**
     * Place a limit sell order
     */
    public OrderResponse limitSell(String symbol, int quantity, BigDecimal price) {
        OrderRequest request = OrderRequest.builder()
            .symbol(symbol)
            .side(OrderSide.SELL)
            .type(OrderType.LIMIT)
            .quantity(quantity)
            .price(price)
            .build();

        return placeOrder(request);
    }
}
package com.bisttrading.broker.service.validation;

import com.bisttrading.broker.algolab.exception.AlgoLabValidationException;
import com.bisttrading.broker.algolab.model.OrderModificationRequest;
import com.bisttrading.broker.algolab.model.OrderSubmissionRequest;
import com.bisttrading.broker.algolab.model.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderValidator {

    @Value("${trading.validation.min-order-value:100.0}")
    private BigDecimal minOrderValue;

    @Value("${trading.validation.max-order-value:1000000.0}")
    private BigDecimal maxOrderValue;

    @Value("${trading.validation.max-quantity:10000}")
    private Integer maxQuantity;

    @Value("${trading.validation.min-price:0.01}")
    private BigDecimal minPrice;

    @Value("${trading.validation.max-price:10000.0}")
    private BigDecimal maxPrice;

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z]{2,6}$");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]{3,50}$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[A-Z0-9]{8,20}$");

    public void validateOrderSubmission(OrderSubmissionRequest request) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        request.validate(); // This throws IllegalArgumentException for basic validation

        try {
            // Validate user ID format
            if (!USER_ID_PATTERN.matcher(request.getUserId()).matches()) {
                errors.add("Invalid user ID format");
            }

            // Validate account ID format
            if (!ACCOUNT_ID_PATTERN.matcher(request.getAccountId()).matches()) {
                errors.add("Invalid account ID format");
            }

            // Validate symbol format
            if (!SYMBOL_PATTERN.matcher(request.getSymbol()).matches()) {
                errors.add("Invalid symbol format. Must be 2-6 uppercase letters");
            }

            // Validate quantity limits
            if (request.getQuantity() > maxQuantity) {
                errors.add(String.format("Quantity exceeds maximum allowed: %d", maxQuantity));
            }

            // Validate price limits
            if (request.getPrice() != null) {
                if (request.getPrice().compareTo(minPrice) < 0) {
                    errors.add(String.format("Price below minimum: %s", minPrice));
                }
                if (request.getPrice().compareTo(maxPrice) > 0) {
                    errors.add(String.format("Price exceeds maximum: %s", maxPrice));
                }
            }

            // Validate stop price limits
            if (request.getStopPrice() != null) {
                if (request.getStopPrice().compareTo(minPrice) < 0) {
                    errors.add(String.format("Stop price below minimum: %s", minPrice));
                }
                if (request.getStopPrice().compareTo(maxPrice) > 0) {
                    errors.add(String.format("Stop price exceeds maximum: %s", maxPrice));
                }
            }

            // Validate order value
            BigDecimal orderValue = request.getEstimatedValue();
            if (orderValue != null) {
                if (orderValue.compareTo(minOrderValue) < 0) {
                    errors.add(String.format("Order value below minimum: %s", minOrderValue));
                }
                if (orderValue.compareTo(maxOrderValue) > 0) {
                    errors.add(String.format("Order value exceeds maximum: %s", maxOrderValue));
                }
            }

            // Validate stop loss logic
            if (request.getOrderType() == OrderType.STOP_LIMIT) {
                validateStopLimitOrder(request, errors);
            }

            // Validate GTD expiration
            if (request.getGoodTillDate() != null) {
                if (request.getGoodTillDate().isBefore(Instant.now().plusSeconds(300))) {
                    errors.add("Good till date must be at least 5 minutes in the future");
                }
            }

            // Validate iceberg order
            if (request.getIcebergQuantity() != null) {
                if (request.getIcebergQuantity() >= request.getQuantity()) {
                    errors.add("Iceberg quantity must be less than total quantity");
                }
                if (request.getIcebergQuantity() <= 0) {
                    errors.add("Iceberg quantity must be positive");
                }
            }

            if (!errors.isEmpty()) {
                throw new AlgoLabValidationException("Order validation failed", errors);
            }

            log.debug("Order validation passed for symbol: {} quantity: {}",
                    request.getSymbol(), request.getQuantity());

        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            throw new AlgoLabValidationException("Order validation failed", errors);
        }
    }

    private void validateStopLimitOrder(OrderSubmissionRequest request, List<String> errors) {
        BigDecimal price = request.getPrice();
        BigDecimal stopPrice = request.getStopPrice();

        if (price == null || stopPrice == null) {
            return; // Basic validation will catch this
        }

        // For buy stop orders, stop price should be above current market price
        // For sell stop orders, stop price should be below current market price
        switch (request.getSide()) {
            case BUY -> {
                if (stopPrice.compareTo(price) <= 0) {
                    errors.add("For buy stop orders, stop price must be above limit price");
                }
            }
            case SELL -> {
                if (stopPrice.compareTo(price) >= 0) {
                    errors.add("For sell stop orders, stop price must be below limit price");
                }
            }
        }
    }

    public void validateOrderCancellation(String orderId, String userId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new AlgoLabValidationException("Order ID is required for cancellation");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new AlgoLabValidationException("User ID is required for cancellation");
        }

        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new AlgoLabValidationException("Invalid user ID format");
        }

        log.debug("Order cancellation validation passed for order: {} user: {}", orderId, userId);
    }

    public void validateOrderModification(String orderId, OrderModificationRequest request) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new AlgoLabValidationException("Order ID is required for modification");
        }

        List<String> errors = new ArrayList<>();

        try {
            // Basic validation from request
            request.validate();

            // Validate new quantity limits
            if (request.getNewQuantity() != null && request.getNewQuantity() > maxQuantity) {
                errors.add(String.format("New quantity exceeds maximum allowed: %d", maxQuantity));
            }

            // Validate new price limits
            if (request.getNewPrice() != null) {
                if (request.getNewPrice().compareTo(minPrice) < 0) {
                    errors.add(String.format("New price below minimum: %s", minPrice));
                }
                if (request.getNewPrice().compareTo(maxPrice) > 0) {
                    errors.add(String.format("New price exceeds maximum: %s", maxPrice));
                }
            }

            // Validate new stop price limits
            if (request.getNewStopPrice() != null) {
                if (request.getNewStopPrice().compareTo(minPrice) < 0) {
                    errors.add(String.format("New stop price below minimum: %s", minPrice));
                }
                if (request.getNewStopPrice().compareTo(maxPrice) > 0) {
                    errors.add(String.format("New stop price exceeds maximum: %s", maxPrice));
                }
            }

            // Validate GTD expiration
            if (request.getNewGoodTillDate() != null) {
                if (request.getNewGoodTillDate().isBefore(Instant.now().plusSeconds(300))) {
                    errors.add("New good till date must be at least 5 minutes in the future");
                }
            }

            if (!errors.isEmpty()) {
                throw new AlgoLabValidationException("Order modification validation failed", errors);
            }

            log.debug("Order modification validation passed for order: {}", orderId);

        } catch (IllegalArgumentException e) {
            errors.add(e.getMessage());
            throw new AlgoLabValidationException("Order modification validation failed", errors);
        }
    }

    // Business rules validation methods
    public boolean isMarketOpen() {
        // This would typically check against market hours
        // For now, simple time-based check (9:30 AM - 6:00 PM Istanbul time)
        return true; // Placeholder implementation
    }

    public boolean isSymbolTradable(String symbol) {
        // Check if symbol is in tradable instruments list
        // This would typically query a database or cache
        return SYMBOL_PATTERN.matcher(symbol).matches();
    }

    public boolean isOrderTypeAllowed(OrderType orderType, String symbol) {
        // Some symbols might not support all order types
        // This is a placeholder implementation
        return true;
    }
}
package com.bisttrading.trading.specification;

import com.bisttrading.entity.UserEntity;
import com.bisttrading.entity.trading.Order;
import com.bisttrading.entity.trading.Symbol;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.trading.dto.OrderSearchCriteria;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for building dynamic Order queries.
 * Uses the Specification pattern for flexible, type-safe queries.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class OrderSpecifications {

    /**
     * Create specification for a specific user.
     *
     * @param userId User ID
     * @return Specification for filtering by user
     */
    public static Specification<Order> forUser(String userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            Join<Order, UserEntity> userJoin = root.join("user");
            return criteriaBuilder.equal(userJoin.get("id"), userId);
        };
    }

    /**
     * Create specification for filtering by symbol.
     *
     * @param symbolCode Symbol code
     * @return Specification for filtering by symbol
     */
    public static Specification<Order> withSymbol(String symbolCode) {
        return (root, query, criteriaBuilder) -> {
            if (symbolCode == null || symbolCode.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<Order, Symbol> symbolJoin = root.join("symbol");
            return criteriaBuilder.equal(
                criteriaBuilder.upper(symbolJoin.get("symbol")),
                symbolCode.toUpperCase()
            );
        };
    }

    /**
     * Create specification for filtering by order status.
     *
     * @param status Order status
     * @return Specification for filtering by status
     */
    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("orderStatus"), status);
        };
    }

    /**
     * Create specification for filtering by multiple order statuses.
     *
     * @param statuses List of order statuses
     * @return Specification for filtering by statuses
     */
    public static Specification<Order> withStatuses(List<OrderStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("orderStatus").in(statuses);
        };
    }

    /**
     * Create specification for filtering by account ID.
     *
     * @param accountId Account ID
     * @return Specification for filtering by account
     */
    public static Specification<Order> withAccountId(String accountId) {
        return (root, query, criteriaBuilder) -> {
            if (accountId == null || accountId.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("accountId"), accountId);
        };
    }

    /**
     * Create specification for filtering by client order ID.
     *
     * @param clientOrderId Client order ID
     * @return Specification for filtering by client order ID
     */
    public static Specification<Order> withClientOrderId(String clientOrderId) {
        return (root, query, criteriaBuilder) -> {
            if (clientOrderId == null || clientOrderId.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("clientOrderId"), clientOrderId);
        };
    }

    /**
     * Create specification for filtering by broker order ID.
     *
     * @param brokerOrderId Broker order ID
     * @return Specification for filtering by broker order ID
     */
    public static Specification<Order> withBrokerOrderId(String brokerOrderId) {
        return (root, query, criteriaBuilder) -> {
            if (brokerOrderId == null || brokerOrderId.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("brokerOrderId"), brokerOrderId);
        };
    }

    /**
     * Create specification for filtering by date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Specification for filtering by date range
     */
    public static Specification<Order> createdBetween(ZonedDateTime startDate, ZonedDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate)
                );
            }

            if (endDate != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate)
                );
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification for filtering by quantity range.
     *
     * @param minQuantity Minimum quantity
     * @param maxQuantity Maximum quantity
     * @return Specification for filtering by quantity range
     */
    public static Specification<Order> withQuantityBetween(Integer minQuantity, Integer maxQuantity) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (minQuantity != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("quantity"), minQuantity)
                );
            }

            if (maxQuantity != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("quantity"), maxQuantity)
                );
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification for filtering active orders only.
     *
     * @return Specification for active orders
     */
    public static Specification<Order> isActive() {
        return (root, query, criteriaBuilder) -> {
            List<OrderStatus> activeStatuses = List.of(
                OrderStatus.SUBMITTED,
                OrderStatus.ACCEPTED,
                OrderStatus.PARTIALLY_FILLED
            );
            return root.get("orderStatus").in(activeStatuses);
        };
    }

    /**
     * Create specification for filtering filled orders only.
     *
     * @return Specification for filled orders
     */
    public static Specification<Order> isFilled() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("orderStatus"), OrderStatus.FILLED);
    }

    /**
     * Create specification for filtering by strategy ID.
     *
     * @param strategyId Strategy ID
     * @return Specification for filtering by strategy
     */
    public static Specification<Order> withStrategyId(String strategyId) {
        return (root, query, criteriaBuilder) -> {
            if (strategyId == null || strategyId.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("strategyId"), strategyId);
        };
    }

    /**
     * Create specification for filtering by algorithm ID.
     *
     * @param algoId Algorithm ID
     * @return Specification for filtering by algorithm
     */
    public static Specification<Order> withAlgoId(String algoId) {
        return (root, query, criteriaBuilder) -> {
            if (algoId == null || algoId.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("algoId"), algoId);
        };
    }

    /**
     * Create comprehensive specification from search criteria.
     *
     * @param criteria Search criteria
     * @return Composite specification
     */
    public static Specification<Order> withCriteria(OrderSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            if (criteria == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // Symbol filter
            if (criteria.getSymbol() != null && !criteria.getSymbol().isEmpty()) {
                Join<Order, Symbol> symbolJoin = root.join("symbol");
                predicates.add(
                    criteriaBuilder.equal(
                        criteriaBuilder.upper(symbolJoin.get("symbol")),
                        criteria.getSymbol().toUpperCase()
                    )
                );
            }

            // Order side filter
            if (criteria.getOrderSide() != null) {
                predicates.add(
                    criteriaBuilder.equal(root.get("orderSide"), criteria.getOrderSide())
                );
            }

            // Order type filter
            if (criteria.getOrderType() != null) {
                predicates.add(
                    criteriaBuilder.equal(root.get("orderType"), criteria.getOrderType())
                );
            }

            // Status filter (single or multiple)
            if (criteria.getOrderStatus() != null) {
                predicates.add(
                    criteriaBuilder.equal(root.get("orderStatus"), criteria.getOrderStatus())
                );
            } else if (criteria.getOrderStatuses() != null && !criteria.getOrderStatuses().isEmpty()) {
                predicates.add(
                    root.get("orderStatus").in(criteria.getOrderStatuses())
                );
            }

            // Account ID filter
            if (criteria.getAccountId() != null && !criteria.getAccountId().isEmpty()) {
                predicates.add(
                    criteriaBuilder.equal(root.get("accountId"), criteria.getAccountId())
                );
            }

            // Client order ID filter
            if (criteria.getClientOrderId() != null && !criteria.getClientOrderId().isEmpty()) {
                predicates.add(
                    criteriaBuilder.equal(root.get("clientOrderId"), criteria.getClientOrderId())
                );
            }

            // Broker order ID filter
            if (criteria.getBrokerOrderId() != null && !criteria.getBrokerOrderId().isEmpty()) {
                predicates.add(
                    criteriaBuilder.equal(root.get("brokerOrderId"), criteria.getBrokerOrderId())
                );
            }

            // Date range filter
            if (criteria.getCreatedAfter() != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAfter())
                );
            }
            if (criteria.getCreatedBefore() != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedBefore())
                );
            }

            // Quantity range filter
            if (criteria.getMinQuantity() != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("quantity"), criteria.getMinQuantity())
                );
            }
            if (criteria.getMaxQuantity() != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("quantity"), criteria.getMaxQuantity())
                );
            }

            // Price range filter
            if (criteria.getMinPrice() != null) {
                predicates.add(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice())
                );
            }
            if (criteria.getMaxPrice() != null) {
                predicates.add(
                    criteriaBuilder.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice())
                );
            }

            // Active only filter
            if (Boolean.TRUE.equals(criteria.getActiveOnly())) {
                List<OrderStatus> activeStatuses = List.of(
                    OrderStatus.SUBMITTED,
                    OrderStatus.ACCEPTED,
                    OrderStatus.PARTIALLY_FILLED
                );
                predicates.add(root.get("orderStatus").in(activeStatuses));
            }

            // Filled only filter
            if (Boolean.TRUE.equals(criteria.getFilledOnly())) {
                predicates.add(
                    criteriaBuilder.equal(root.get("orderStatus"), OrderStatus.FILLED)
                );
            }

            // Strategy ID filter
            if (criteria.getStrategyId() != null && !criteria.getStrategyId().isEmpty()) {
                predicates.add(
                    criteriaBuilder.equal(root.get("strategyId"), criteria.getStrategyId())
                );
            }

            // Algorithm ID filter
            if (criteria.getAlgoId() != null && !criteria.getAlgoId().isEmpty()) {
                predicates.add(
                    criteriaBuilder.equal(root.get("algoId"), criteria.getAlgoId())
                );
            }

            // Default ordering by creation date descending
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

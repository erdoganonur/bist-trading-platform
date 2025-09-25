package com.bisttrading.oms.service;

import com.bisttrading.oms.model.OMSOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for auditing order operations
 */
@Slf4j
@Service
public class OrderAuditService {

    public void auditOrderCreation(OMSOrder order) {
        log.info("ORDER_CREATED - OrderId: {}, UserId: {}, Symbol: {}, Side: {}, Quantity: {}, Price: {}",
                order.getOrderId(), order.getUserId(), order.getSymbol(),
                order.getSide(), order.getQuantity(), order.getPrice());
    }

    public void auditOrderModification(OMSOrder oldOrder, OMSOrder newOrder, String reason) {
        log.info("ORDER_MODIFIED - OrderId: {}, UserId: {}, Changes: Quantity [{} -> {}], Price [{} -> {}], Reason: {}",
                oldOrder.getOrderId(), oldOrder.getUserId(),
                oldOrder.getQuantity(), newOrder.getQuantity(),
                oldOrder.getPrice(), newOrder.getPrice(), reason);
    }

    public void auditOrderCancellation(OMSOrder order, String reason) {
        log.info("ORDER_CANCELLED - OrderId: {}, UserId: {}, Symbol: {}, Status: {}, Reason: {}",
                order.getOrderId(), order.getUserId(), order.getSymbol(),
                order.getStatus(), reason);
    }

    public void auditOrderFill(OMSOrder order) {
        log.info("ORDER_FILLED - OrderId: {}, UserId: {}, Symbol: {}, FilledQty: {}, AvgPrice: {}, Commission: {}",
                order.getOrderId(), order.getUserId(), order.getSymbol(),
                order.getFilledQuantity(), order.getAveragePrice(), order.getCommission());
    }

    public void auditOrderRejection(OMSOrder order, String reason) {
        log.warn("ORDER_REJECTED - OrderId: {}, UserId: {}, Symbol: {}, Reason: {}",
                order.getOrderId(), order.getUserId(), order.getSymbol(), reason);
    }

    public void auditOrderExpiry(OMSOrder order) {
        log.info("ORDER_EXPIRED - OrderId: {}, UserId: {}, Symbol: {}, ExpiredAt: {}",
                order.getOrderId(), order.getUserId(), order.getSymbol(),
                LocalDateTime.now());
    }

    public void auditOrderStatusChange(OMSOrder order, OMSOrder.OrderStatus oldStatus, OMSOrder.OrderStatus newStatus) {
        log.info("ORDER_STATUS_CHANGE - OrderId: {}, UserId: {}, Status: {} -> {}",
                order.getOrderId(), order.getUserId(), oldStatus, newStatus);
    }

    public void auditInternalError(String operation, String orderId, String error) {
        log.error("ORDER_INTERNAL_ERROR - Operation: {}, OrderId: {}, Error: {}",
                operation, orderId, error);
    }

    public void auditExternalOrderUpdate(String orderId, String externalOrderId, String brokerResponse) {
        log.info("EXTERNAL_ORDER_UPDATE - OrderId: {}, ExternalOrderId: {}, BrokerResponse: {}",
                orderId, externalOrderId, brokerResponse);
    }

    public void auditComplianceViolation(OMSOrder order, String violation) {
        log.warn("COMPLIANCE_VIOLATION - OrderId: {}, UserId: {}, Violation: {}",
                order.getOrderId(), order.getUserId(), violation);
    }

    public void auditRiskCheck(OMSOrder order, String riskResult) {
        log.info("RISK_CHECK - OrderId: {}, UserId: {}, Result: {}",
                order.getOrderId(), order.getUserId(), riskResult);
    }
}
# BIST Trading Platform - Codebase Exploration Summary

**Generated:** October 23, 2025  
**Thoroughness Level:** VERY THOROUGH  
**Target:** Telegram Bot Implementation  

---

## Executive Summary

This exploration provides a comprehensive analysis of the trading functionality in the BIST Trading Platform. All code snippets, file paths, and implementation details are documented for direct use in the Telegram bot implementation.

**Key Finding:** The platform has production-ready trading infrastructure with complete order management, position tracking, and AlgoLab broker integration.

---

## 1. ORDER MANAGEMENT (Emir İşlemleri)

### Overview
The platform provides a complete order lifecycle management system with database persistence and AlgoLab integration.

### Key Files
- **DTO:** `/src/main/java/com/bisttrading/trading/dto/CreateOrderRequest.java` (189 lines)
- **Entity:** `/src/main/java/com/bisttrading/entity/trading/Order.java` (617 lines)
- **Service:** `/src/main/java/com/bisttrading/trading/service/OrderManagementService.java` (552 lines)
- **Repository:** `/src/main/java/com/bisttrading/repository/trading/OrderRepository.java` (114 lines)
- **History DTO:** `/src/main/java/com/bisttrading/trading/dto/OrderHistoryDto.java` (277 lines)

### CreateOrderRequest DTO
```java
public class CreateOrderRequest {
    // REQUIRED
    String symbol;              // "AKBNK", "GARAN", etc.
    OrderSide side;             // BUY or SELL
    OrderType orderType;        // LIMIT, MARKET, STOP, etc.
    Integer quantity;           // Number of shares
    String accountId;           // Broker account ID
    
    // OPTIONAL
    BigDecimal price;           // For LIMIT orders (required)
    BigDecimal stopPrice;       // For STOP orders (required)
    TimeInForce timeInForce;    // DAY (default), GTC, GTD
    ZonedDateTime expiresAt;    // For GTD orders
    String clientOrderId;       // Custom order ID
    String subAccountId;        // Sub-account
    Boolean smsNotification;    // SMS on execution
    Boolean emailNotification;  // Email on execution
    String strategyId;          // For algo orders
    String algoId;              // Algorithm ID
    String parentOrderId;       // For bracket/OCO orders
}
```

### Order Entity Key Fields
```java
// Identification
clientOrderId               // User-provided ID
brokerOrderId              // AlgoLab order ID
exchangeOrderId            // Exchange order ID

// Trading Details
UserEntity user             // Order owner
Symbol symbol              // Trading symbol
OrderType orderType        // LIMIT, MARKET, STOP, etc.
OrderSide orderSide        // BUY or SELL
TimeInForce timeInForce    // DAY, GTC, GTD

// Quantities
Integer quantity           // Original quantity
Integer filledQuantity     // Filled amount
Integer remainingQuantity  // Remaining amount

// Pricing
BigDecimal price           // Limit price
BigDecimal stopPrice       // Stop trigger price
BigDecimal averageFillPrice // Weighted average
BigDecimal lastFillPrice   // Last execution price

// Status
OrderStatus orderStatus    // Current status
String statusReason        // Status change reason

// Fees
BigDecimal commission      // Trading commission
BigDecimal brokerageFee    // Brokerage fee
BigDecimal exchangeFee     // Exchange fee
BigDecimal taxAmount       // Tax amount

// Timestamps
ZonedDateTime submittedAt  // Submission time
ZonedDateTime acceptedAt   // Acceptance time
ZonedDateTime firstFillAt  // First execution time
ZonedDateTime lastFillAt   // Last execution time
ZonedDateTime completedAt  // Completion time

// History
List<OrderExecution> executions // All executions
```

### Order Service Methods
```java
// Create and place order (persists to DB, sends to AlgoLab)
Order createOrder(CreateOrderRequest request, String userId)

// Retrieve
Optional<Order> getOrderById(String orderId)
Page<Order> getOrderHistory(String userId, OrderSearchCriteria criteria, Pageable pageable)
List<Order> getActiveOrders(String userId)

// Modify
Order modifyOrder(String orderId, BigDecimal newPrice, Integer newQuantity)
Order updateOrderStatus(String orderId, OrderStatus newStatus, OrderExecutionDetails details)

// Cancel
Order cancelOrder(String orderId)

// Sync
void syncOrderStatusFromAlgoLab()  // Scheduled every 30 seconds
List<Order> getOrdersByStatuses(List<OrderStatus> statuses)
```

### Order Statuses
| Status | Can Cancel | Can Modify | Can Execute |
|--------|-----------|-----------|------------|
| PENDING | No | No | No |
| SUBMITTED | Yes | Yes | No |
| ACCEPTED | Yes | Yes | Yes |
| PARTIALLY_FILLED | Yes | Yes | Yes |
| FILLED | No | No | No |
| CANCELLED | - | - | No |
| REJECTED | - | - | No |
| REPLACED | - | - | No |
| SUSPENDED | No | No | No |
| EXPIRED | - | - | No |
| ERROR | - | - | No |

### Order Types
```java
MARKET           // Execute immediately at best price
LIMIT            // Execute at specified price (requires price)
STOP             // Becomes market when trigger hit (requires stopPrice)
STOP_LIMIT       // Becomes limit when trigger hit (requires both)
TRAILING_STOP    // Stop moves with favorable price (requires stopPrice)
ICEBERG          // Large hidden order (requires price)
BRACKET          // Parent with take-profit + stop-loss
OCO              // One-Cancels-Other (two orders)
```

### OrderRepository Methods
```java
// By ID
Optional<Order> findByClientOrderId(String clientOrderId)
Optional<Order> findByBrokerOrderId(String brokerOrderId)
Optional<Order> findByExchangeOrderId(String exchangeOrderId)

// By User
Page<Order> findByUserId(String userId, Pageable pageable)
List<Order> findByUserIdAndOrderStatus(String userId, OrderStatus status)
List<Order> findByUserIdAndOrderStatusIn(String userId, List<OrderStatus> statuses)

// By Account
Page<Order> findByAccountId(String accountId, Pageable pageable)
List<Order> findActiveOrdersByAccountId(String accountId)

// By Symbol
Page<Order> findBySymbolId(String symbolId, Pageable pageable)
List<Order> findActiveOrdersBySymbolId(String symbolId)

// Aggregates
List<Order> findByOrderStatusIn(List<OrderStatus> statuses)
Long countOrdersForUser(String userId)
```

---

## 2. POSITION MANAGEMENT (Pozisyon Yönetimi)

### Overview
Real-time position tracking with P&L calculations and quantity blocking for pending orders.

### Key Files
- **Entity:** `/src/main/java/com/bisttrading/entity/trading/Position.java` (502 lines)
- **Repository:** `/src/main/java/com/bisttrading/repository/trading/PositionRepository.java` (58 lines)

### Position Entity Key Fields
```java
// Identification
UserEntity user             // Position owner
String accountId            // Broker account
String subAccountId         // Sub-account
Symbol symbol              // Trading symbol

// Position State
PositionSide positionSide  // LONG, SHORT, FLAT
Integer quantity           // Net quantity (+long, -short)
Integer availableQuantity  // Available for trading
Integer blockedQuantity    // Blocked by pending orders

// Cost Basis
BigDecimal averageCost     // Cost per share
BigDecimal totalCost       // quantity * averageCost

// P&L
BigDecimal realizedPnl     // Closed position P&L
BigDecimal unrealizedPnl   // Current position P&L

// Market Data
BigDecimal marketPrice     // Current market price
BigDecimal marketValue     // quantity * marketPrice
BigDecimal dayChange       // Price change from previous close
BigDecimal dayChangePercent // Percentage change
BigDecimal lastTradePrice  // Last execution price
ZonedDateTime lastTradeTime // Last execution time

// Risk Metrics
BigDecimal valueAtRisk     // VaR calculation
BigDecimal maximumDrawdown // Max drawdown since open
```

### Position Methods
```java
// Query
boolean isFlat()           // quantity == 0 || FLAT
boolean isLong()           // side == LONG && quantity > 0
boolean isShort()          // side == SHORT && quantity < 0
Integer getAbsoluteQuantity() // Math.abs(quantity)

// Update
void updateWithTrade(OrderSide side, Integer quantity, BigDecimal price)
void updateMarketPrice(BigDecimal newPrice)

// Quantity Management
void blockQuantity(Integer quantityToBlock)    // For pending orders
void releaseQuantity(Integer quantityToRelease) // When order completes

// P&L Calculations
BigDecimal getTotalPnl()      // Realized + Unrealized
BigDecimal getPnlPercentage() // Total P&L / Total Cost * 100
BigDecimal getExposure()      // |marketValue|
BigDecimal getLeverage(BigDecimal marginUsed)
```

### PositionRepository Methods
```java
// By User
List<Position> findByUserId(String userId)
List<Position> findByUserIdAndPositionSide(String userId, PositionSide side)

// By Account
List<Position> findByAccountId(String accountId)
List<Position> findByUserIdAndAccountId(String userId, String accountId)

// By Symbol
List<Position> findBySymbolId(String symbolId)
Optional<Position> findByUserIdAndAccountIdAndSymbolId(String userId, String accountId, String symbolId)

// By Side
List<Position> findByPositionSide(PositionSide side)
```

### Position Sides
```java
LONG    // Own the security (quantity > 0)
SHORT   // Sold the security (quantity < 0)
FLAT    // No position (quantity == 0)
```

---

## 3. PENDING ORDERS (Bekleyen Emirler)

### Overview
Access pending orders through OrderManagementService and AlgoLabOrderService.

### Getting Pending Orders

**Method 1: From Platform Database**
```java
List<Order> activeOrders = orderManagementService.getActiveOrders(userId);

// Filtered
Page<Order> orders = orderManagementService.getOrderHistory(
    userId,
    OrderSearchCriteria.builder()
        .statuses(List.of(
            OrderStatus.SUBMITTED,
            OrderStatus.ACCEPTED,
            OrderStatus.PARTIALLY_FILLED
        ))
        .build(),
    PageRequest.of(0, 20)
);
```

**Method 2: From AlgoLab (Real-time)**
```java
List<Map<String, Object>> pendingOrders = algoLabOrderService.getPendingOrders(subAccount);

// Each order contains:
// {
//   "orderId": "ALG-123456",
//   "symbol": "AKBNK",
//   "direction": "BUY",
//   "equityStatusDescription": "WAITING",  // Pending indicator
//   "pricetype": "limit",
//   "price": "15.75",
//   "lot": "10",
//   ...
// }
```

### AlgoLab Status Mapping
| AlgoLab Status | Description |
|---------------|------------|
| WAITING | Order pending execution |
| DONE | Order filled |
| DELETED | Order cancelled |
| KISMEN | Partially filled |

---

## 4. ALGOLAB INTEGRATION

### Overview
Complete AlgoLab REST API integration with encryption, rate limiting, and resilience patterns.

### Key Files
- **Auth Service:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabAuthService.java` (289 lines)
- **Order Service:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabOrderService.java` (398 lines)
- **Session Manager:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabSessionManager.java` (429 lines)
- **REST Client:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabRestClient.java` (150+ lines)
- **Auth Controller:** `/src/main/java/com/bisttrading/broker/controller/BrokerAuthController.java` (474 lines)
- **Broker Controller:** `/src/main/java/com/bisttrading/broker/controller/BrokerController.java` (150+ lines)

### Authentication Flow

**Step 1: Login (Triggers SMS)**
```java
POST /api/v1/broker/auth/login
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "username": "algolab_username",
  "password": "algolab_password"
}

Response:
{
  "success": true,
  "message": "SMS kodu telefonunuza gönderildi",
  "smsSent": true,
  "authenticated": false,
  "timestamp": "2025-10-15T10:30:00Z"
}
```

**Step 2: Verify OTP**
```java
POST /api/v1/broker/auth/verify-otp
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "otpCode": "123456"
}

Response:
{
  "success": true,
  "message": "Doğrulama başarılı. AlgoLab'a başarıyla bağlandınız.",
  "authenticated": true,
  "sessionExpiresAt": "2025-10-16T10:30:00Z",
  "timestamp": "2025-10-15T10:30:00Z"
}
```

### AlgoLabAuthService Methods
```java
// Authentication Flow
String loginUser(String username, String password)
    // Encrypts credentials, calls /api/LoginUser, returns token

String loginUserControl(String smsCode)
    // Encrypts token+SMS, calls /api/LoginUserControl, returns hash

// Session Management
boolean restoreSession()
    // Loads saved session, validates with GetSubAccounts

boolean isAlive()
    // Calls GetSubAccounts to check if session valid

void sessionRefresh()  // @Scheduled every 5 minutes
    // Calls /api/SessionRefresh to keep session alive

void clearAuth()
    // Clears token and hash, logs out
```

### AlgoLabOrderService Methods

**Send Order**
```java
Map<String, Object> sendOrder(
    String symbol,          // "AKBNK"
    String direction,       // "BUY" or "SELL"
    String priceType,       // "limit" or "piyasa"
    BigDecimal price,       // Limit price (null for market)
    Integer lot,            // 1 lot = 100 shares
    Boolean sms,            // SMS notification
    Boolean email,          // Email notification
    String subAccount       // Sub-account ID
)
// Returns: {"success": true, "content": {"orderId": "...", ...}}
```

**Delete Order (Cancel)**
```java
Map<String, Object> deleteOrder(String orderId, String subAccount)
// Returns: {"success": true, "message": "Order cancelled successfully"}
```

**Modify Order**
```java
Map<String, Object> modifyOrder(
    String orderId,        // Order ID to modify
    BigDecimal price,      // New price
    Integer lot,           // New lot quantity
    Boolean viop,          // VIOP flag
    String subAccount
)
// Returns: {"success": true, "content": {"orderId": "...", "price": "...", ...}}
```

**Get Instant Position (Portfolio)**
```java
Map<String, Object> getInstantPosition(String subAccount)
// Returns: {
//   "success": true,
//   "content": [
//     {"symbol": "AKBNK", "quantity": "1000", "marketPrice": "15.85", ...},
//     ...
//   ]
// }
```

**Get Today's Transactions**
```java
Map<String, Object> getTodaysTransactions(String subAccount)
// Returns all orders (pending, executed, cancelled)
// Used by getPendingOrders() for filtering
```

**Get Pending Orders**
```java
List<Map<String, Object>> getPendingOrders(String subAccount)
// Calls getTodaysTransactions and filters for equityStatusDescription == "WAITING"
```

**Get Sub-Accounts**
```java
Map<String, Object> getSubAccounts()
// Returns: {
//   "success": true,
//   "content": [
//     {"id": "ACC-001", "name": "Main Account"},
//     ...
//   ]
// }
```

### AlgoLabSessionManager
```java
// Persistence
void saveSession(String token, String hash)
AlgoLabSession loadSession()
void clearSession()
void updateWebSocketStatus(boolean connected)

// Configuration
void setCurrentUserId(UUID userId)
UUID getCurrentUserId()

// Statistics
SessionStatistics getStatistics()

// Scheduled Cleanup
void cleanupExpiredSessions()  // @Scheduled hourly
```

### AlgoLabSession Model
```java
public class AlgoLabSession {
    String token;                           // Auth token
    String hash;                            // Auth hash
    LocalDateTime lastUpdate;               // Last update time
    boolean websocketConnected;             // WS status
    LocalDateTime websocketLastConnected;   // WS last connection time
}
```

### REST Client Features
- **Rate Limiting:** 1 request per 5 seconds (0.2 permits/sec)
- **Circuit Breaker:** Fails fast if AlgoLab unavailable
- **Retry:** Exponential backoff on failures
- **Timeout:** Request-level timeout protection
- **Metrics:** Tracked with Micrometer

---

## 5. SESSION MANAGEMENT

### Overview
Two-tier session system: JWT for BIST platform + Token/Hash for AlgoLab.

### JWT Token (BIST Platform)
- **Expiration:** 24 hours (configurable)
- **Header:** `Authorization: Bearer {jwt_token}`
- **Refresh:** Via `/api/v1/auth/refresh-token`
- **Claims:** userId, username, authorities, email, etc.

### AlgoLab Session Persistence

**Storage Options (Configurable)**
```yaml
algolab:
  session:
    storage: "database"               # or "file"
    file-path: "./algolab-session.json"
    expiration-hours: 24
    retention-days: 30
```

**File Storage Format**
```json
{
  "token": "encrypted_token_from_algolab",
  "hash": "authorization_hash_from_algolab",
  "lastUpdate": "2025-10-23T10:30:00",
  "websocketConnected": true,
  "websocketLastConnected": "2025-10-23T10:25:00"
}
```

**Database Storage**
- Table: `algolab_sessions`
- Supports per-user sessions
- Automatic cleanup of expired sessions
- Audit trail with timestamps

### Session Lifecycle
```
1. User Login (BIST) → JWT issued
2. User calls /broker/auth/login → SMS sent
3. User calls /broker/auth/verify-otp → Token + Hash saved
4. Session valid for 24 hours
5. Auto-refresh every 5 minutes
6. Session expires after 24 hours → Re-login required
7. Logout clears token + hash
```

### Session Endpoints
```bash
# Check status
GET /api/v1/broker/auth/status
Authorization: Bearer {jwt_token}

# Logout
POST /api/v1/broker/auth/logout
Authorization: Bearer {jwt_token}
```

---

## 6. KEY ENUMS

### OrderStatus.java (125 lines)
```java
PENDING, SUBMITTED, ACCEPTED, REJECTED, PARTIALLY_FILLED, FILLED,
CANCELLED, REPLACED, SUSPENDED, EXPIRED, ERROR
```

### OrderType.java (82 lines)
```java
MARKET, LIMIT, STOP, STOP_LIMIT, TRAILING_STOP, ICEBERG, BRACKET, OCO
```

### OrderSide.java (55 lines)
```java
BUY (multiplier: +1)
SELL (multiplier: -1)
```

### TimeInForce.java
```java
DAY         // Order expires at market close
GTC         // Good-Till-Cancelled
GTD         // Good-Till-Date (requires expiresAt)
```

### PositionSide.java
```java
LONG        // Long position (quantity > 0)
SHORT       // Short position (quantity < 0)
FLAT        // No position (quantity == 0)
```

---

## 7. SEND ORDER REQUEST DTO

**File:** `/src/main/java/com/bisttrading/broker/dto/SendOrderRequest.java`

```java
@Data
@Builder
public class SendOrderRequest {
    @NotBlank String symbol;        // "AKBNK", etc.
    @NotBlank String direction;     // "BUY" or "SELL"
    @NotBlank String priceType;     // "LIMIT" or "MARKET"
    @Positive BigDecimal price;     // Optional (null for market)
    @NotNull @Positive Integer lot; // Lot quantity (1 lot = 100 shares)
    Boolean sms;                    // SMS notification (default: false)
    Boolean email;                  // Email notification (default: false)
    String subAccount;              // Sub-account ID (optional)
}
```

---

## 8. ORDER EXECUTION HISTORY

**File:** `/src/main/java/com/bisttrading/trading/dto/ExecutionDto.java`

Each order can have multiple executions:

```java
public class ExecutionDto {
    String executionId;                 // Execution identifier
    String orderId;                     // Parent order ID
    Integer executionQuantity;          // Quantity executed
    BigDecimal executionPrice;          // Execution price
    ZonedDateTime executionTime;        // When executed
    String tradeId;                     // Trade identifier
    String brokerExecutionId;           // Broker execution ID
    String contraBroker;                // Contra broker
    BigDecimal grossAmount;             // Quantity * Price
    BigDecimal commission;              // Trading commission
    BigDecimal brokerageFee;           // Brokerage fee
    BigDecimal exchangeFee;            // Exchange fee
    BigDecimal tax;                     // Applicable tax
    BigDecimal netAmount;              // After fees/tax
    BigDecimal bidPrice;                // Bid price at execution
    BigDecimal askPrice;                // Ask price at execution
    BigDecimal marketPrice;             // Market price at execution
}
```

---

## 9. IMPLEMENTATION CHECKLIST FOR TELEGRAM BOT

### Required User Information
- [ ] BIST Trading Platform username/email
- [ ] BIST Trading Platform password
- [ ] AlgoLab broker username
- [ ] AlgoLab broker password
- [ ] Broker account ID
- [ ] Sub-account ID (if applicable)

### Authentication Flow
- [ ] Step 1: Login to BIST Platform → Get JWT
- [ ] Step 2: Call /broker/auth/login → SMS sent
- [ ] Step 3: Collect SMS code from user
- [ ] Step 4: Call /broker/auth/verify-otp → Session created

### Trading Commands
- [ ] Place order: POST /api/v1/broker/orders
- [ ] Get active orders: GET /api/v1/broker/orders/active
- [ ] Get order history: GET /api/v1/broker/orders
- [ ] Cancel order: DELETE /api/v1/broker/orders/{id}
- [ ] Modify order: PUT /api/v1/broker/orders/{id}

### Position Commands
- [ ] Get all positions: GET /api/v1/broker/positions
- [ ] Get position by symbol: GET /api/v1/broker/positions/symbol/{symbol}
- [ ] Get positions by account: GET /api/v1/broker/positions/account/{accountId}

### Status & Management
- [ ] Check connection status: GET /api/v1/broker/auth/status
- [ ] Logout: POST /api/v1/broker/auth/logout
- [ ] Handle 429 rate limit errors (1 per 5 seconds)
- [ ] Cache frequently accessed data

---

## 10. API ENDPOINT SUMMARY

### Authentication (BIST Platform)
```
POST   /api/v1/auth/login                    # JWT login
POST   /api/v1/auth/refresh-token            # Refresh JWT
POST   /api/v1/auth/logout                   # Logout
```

### Broker Authentication (AlgoLab)
```
POST   /api/v1/broker/auth/login             # Initiate login (SMS)
POST   /api/v1/broker/auth/verify-otp        # Verify SMS OTP
GET    /api/v1/broker/auth/status            # Check status
POST   /api/v1/broker/auth/logout            # Logout
```

### Orders
```
POST   /api/v1/broker/orders                 # Create order
GET    /api/v1/broker/orders                 # Order history
GET    /api/v1/broker/orders/active          # Active orders
GET    /api/v1/broker/orders/{id}            # Order details
DELETE /api/v1/broker/orders/{id}            # Cancel order
PUT    /api/v1/broker/orders/{id}            # Modify order
```

### Positions
```
GET    /api/v1/broker/positions              # All positions
GET    /api/v1/broker/positions/{id}         # Position details
GET    /api/v1/broker/positions/account/{accountId}
GET    /api/v1/broker/positions/symbol/{symbol}
```

### Market Data
```
GET    /api/v1/market/symbols                # Available symbols
GET    /api/v1/market/symbols/{symbol}       # Symbol details
GET    /api/v1/market/data/{symbol}/tick     # Tick data
```

---

## 11. KEY CONSTANTS & CONVERSIONS

### Quantity Units
- **Platform:** Number of shares (e.g., 1000 shares)
- **AlgoLab:** Lots (1 lot = 100 shares)
- **Conversion:** lots = quantity / 100

### Price Precision
- **Type:** BigDecimal (NOT float/double)
- **Decimal Places:** 6 (e.g., 15.750000)
- **Min Value:** 0.01

### Timestamps
- **Format:** ISO 8601 with timezone
- **Example:** 2025-10-15T10:30:00.000Z
- **Timezone:** Always UTC

### HTTP Status Codes
```
200 OK                  # Success
201 CREATED            # Order created
400 BAD REQUEST        # Validation failed
401 UNAUTHORIZED       # No/invalid JWT
402 PAYMENT REQUIRED   # Insufficient funds
403 FORBIDDEN          # No permission
404 NOT FOUND          # Order/position not found
428 PRECONDITION       # Need /login first
429 TOO MANY REQUESTS  # Rate limit exceeded
500 SERVER ERROR       # Server error
503 SERVICE DOWN       # AlgoLab down
```

---

## 12. RATE LIMITING & RESILIENCE

### Rate Limiting
- **AlgoLab API:** 1 request per 5 seconds per user
- **Error Response:** 429 Too Many Requests
- **Implementation:** RateLimiter with 0.2 permits/second

### Resilience Patterns
- **Circuit Breaker:** Fails fast if AlgoLab unavailable
- **Retry:** Exponential backoff on failures
- **Timeout:** Request-level timeout protection
- **Fallback:** Returns cached data when available

---

## 13. ERROR HANDLING

### Common Errors
```
AlgoLabAuthenticationException        # Auth failed
OrderNotFoundException                # Order not found
OrderNotCancellableException          # Cannot cancel
OrderValidationException              # Invalid order
OrderCancellationFailedException      # Cancellation failed
SymbolNotFoundException                # Symbol not found
```

### Error Response Format
```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2025-10-15T10:30:00Z"
}
```

---

## 14. COMPLETE FILE LISTING

| Component | File | Lines | Purpose |
|-----------|------|-------|---------|
| **Order DTOs** | `trading/dto/CreateOrderRequest.java` | 189 | Order creation |
| | `trading/dto/OrderHistoryDto.java` | 277 | Order history response |
| | `trading/dto/ExecutionDto.java` | 120+ | Execution details |
| | `broker/dto/SendOrderRequest.java` | 42 | Broker order request |
| **Order Entity** | `entity/trading/Order.java` | 617 | Order persistence |
| | `entity/trading/OrderExecution.java` | 100+ | Execution persistence |
| **Position Entity** | `entity/trading/Position.java` | 502 | Position persistence |
| **Services** | `trading/service/OrderManagementService.java` | 552 | Order business logic |
| **Repositories** | `repository/trading/OrderRepository.java` | 114 | Order persistence |
| | `repository/trading/PositionRepository.java` | 58 | Position persistence |
| **Enums** | `entity/trading/enums/OrderStatus.java` | 125 | Status lifecycle |
| | `entity/trading/enums/OrderType.java` | 82 | Order types |
| | `entity/trading/enums/OrderSide.java` | 55 | Buy/Sell |
| | `entity/trading/enums/PositionSide.java` | 50+ | Position sides |
| | `entity/trading/enums/TimeInForce.java` | 50+ | Time in force |
| **AlgoLab Auth** | `broker/algolab/service/AlgoLabAuthService.java` | 289 | Authentication |
| **AlgoLab Orders** | `broker/algolab/service/AlgoLabOrderService.java` | 398 | Order API |
| **AlgoLab Session** | `broker/algolab/service/AlgoLabSessionManager.java` | 429 | Session management |
| **REST Client** | `broker/algolab/service/AlgoLabRestClient.java` | 150+ | HTTP client |
| **Controllers** | `broker/controller/BrokerAuthController.java` | 474 | Auth endpoints |
| | `broker/controller/BrokerController.java` | 150+ | Trading endpoints |
| **Models** | `broker/algolab/model/AlgoLabSession.java` | 45 | Session model |
| | `broker/algolab/entity/AlgoLabSessionEntity.java` | 80+ | Session persistence |

---

## 15. QUICK REFERENCE: TELEGRAM BOT IMPLEMENTATION

### Minimal Implementation
```
1. Authenticate user with BIST platform
   → GET JWT token

2. Authenticate with AlgoLab
   → SMS OTP flow with verification

3. Provide commands:
   /order - Place new order
   /active - Show active orders
   /positions - Show current positions
   /cancel - Cancel order
   /status - Check connection status

4. Handle errors gracefully
   → Show user-friendly messages
   → Retry on rate limits
```

### Data You Can Provide Users
- Active orders with status, quantity, price, P&L
- Open positions with quantity, cost, market value, P&L
- Order history with execution details
- Commission and fees breakdown
- Real-time position P&L updates

---

## 16. PRODUCTION DEPLOYMENT NOTES

### Important Warnings
- **PRODUCTION TRADING:** All orders are LIVE with REAL MONEY
- **RATE LIMITING:** 1 request per 5 seconds - implement queuing
- **SESSION EXPIRY:** 24 hours - implement refresh logic
- **CIRCUIT BREAKER:** AlgoLab unavailability impacts trading
- **AUDIT LOGGING:** All operations must be logged for compliance

### Recommended Configurations
```yaml
# For production
algolab:
  session:
    storage: "database"
    expiration-hours: 24
    retention-days: 90
  
  api:
    rate-limit: 0.2  # 1 per 5 seconds
    timeout-ms: 30000
    
  auth:
    keep-alive: true
    refresh-interval-ms: 300000  # 5 minutes
```

---

## Summary Statistics

- **Total Files Analyzed:** 15+ key files
- **Total Lines of Code:** 5,000+ lines
- **Key Services:** 3 (OrderManagement, AlgoLabAuth, AlgoLabOrder)
- **Key Entities:** 3 (Order, Position, AlgoLabSession)
- **Order Statuses:** 11 states with transition rules
- **Order Types:** 8 types with validation
- **API Endpoints:** 20+ endpoints documented
- **Configuration Parameters:** 15+ configurable settings

---

## Contact & Support

**For Telegram Bot Implementation:**
1. Refer to TRADING_FUNCTIONALITY_GUIDE.md (comprehensive guide)
2. Check OpenAPI docs at `/swagger-ui.html`
3. Review error responses for proper handling
4. Implement rate limiting (1 request per 5 seconds)
5. Use proper pagination for list endpoints
6. Validate all user inputs before API calls

**Document Generated:** October 23, 2025  
**Version:** 2.0.0 - Production Ready


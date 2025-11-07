# BIST Trading Platform - Complete Trading Functionality Documentation

**Version:** 2.0.0  
**Last Updated:** October 23, 2025  
**Author:** BIST Trading Platform  
**Target:** Telegram Bot Implementation  

---

## Table of Contents

1. [Overview](#overview)
2. [Order Management (Emir İşlemleri)](#order-management)
3. [Position Management (Pozisyon Yönetimi)](#position-management)
4. [Pending Orders (Bekleyen Emirler)](#pending-orders)
5. [AlgoLab Integration](#algolab-integration)
6. [Session Management](#session-management)
7. [Implementation Guide for Telegram Bot](#implementation-guide)
8. [API Endpoints](#api-endpoints)

---

## Overview

The BIST Trading Platform provides a complete trading infrastructure with:
- **Production-ready order management** with database persistence
- **AlgoLab broker integration** for real-time order execution
- **Position tracking** with real-time P&L calculations
- **Session persistence** supporting both file and database storage
- **WebSocket support** for real-time market data and order updates

### Key Technologies
- **Backend:** Java 17+ with Spring Boot 3.x
- **Database:** PostgreSQL (JPA/Hibernate)
- **Trading Integration:** AlgoLab REST API + WebSocket
- **Authentication:** JWT tokens with role-based access control

---

## 1. Order Management (Emir İşlemleri)

### 1.1 Order Lifecycle

```
PENDING → SUBMITTED → ACCEPTED → PARTIALLY_FILLED → FILLED
           ↓            ↓            ↓                 ↓
         REJECTED    CANCELLED   CANCELLED        (Complete)
```

### 1.2 Create Order Request DTO

**File:** `/src/main/java/com/bisttrading/trading/dto/CreateOrderRequest.java`

```java
@Data
@Builder
public class CreateOrderRequest {
    // Required fields
    @NotBlank String symbol;           // Symbol code (e.g., "AKBNK", "GARAN")
    @NotNull OrderSide side;           // BUY or SELL
    @NotNull OrderType orderType;      // LIMIT, MARKET, STOP, etc.
    @NotNull Integer quantity;         // Number of shares
    @NotBlank String accountId;        // Broker account ID
    
    // Optional fields
    BigDecimal price;                  // For LIMIT orders (required)
    BigDecimal stopPrice;              // For STOP orders (required)
    TimeInForce timeInForce;           // DAY, GTC, GTD (default: DAY)
    ZonedDateTime expiresAt;           // For GTD orders
    String clientOrderId;              // Custom order ID
    String subAccountId;               // Sub-account (optional)
    Boolean smsNotification;           // Notify on execution
    Boolean emailNotification;         // Email notification
    String strategyId;                 // For algorithmic orders
    String algoId;                     // Algorithm identifier
    String parentOrderId;              // For bracket/OCO orders
}
```

### 1.3 Order Entity Structure

**File:** `/src/main/java/com/bisttrading/entity/trading/Order.java`

**Key Fields:**
```java
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    // Order Identification
    String clientOrderId;              // User-provided ID
    String brokerOrderId;              // AlgoLab order ID
    String exchangeOrderId;            // Exchange order ID
    
    // Trading Details
    UserEntity user;                   // Order owner
    Symbol symbol;                     // Trading symbol
    OrderType orderType;               // LIMIT, MARKET, STOP, etc.
    OrderSide orderSide;               // BUY or SELL
    TimeInForce timeInForce;           // DAY, GTC, GTD
    
    // Quantities
    Integer quantity;                  // Original quantity
    Integer filledQuantity;            // Filled amount
    Integer remainingQuantity;         // Remaining amount
    
    // Pricing
    BigDecimal price;                  // Limit price
    BigDecimal stopPrice;              // Stop trigger price
    BigDecimal averageFillPrice;       // Weighted average price
    BigDecimal lastFillPrice;          // Last execution price
    
    // Status Tracking
    OrderStatus orderStatus;           // Current status
    String statusReason;               // Status change reason
    
    // Fees and Costs
    BigDecimal commission;             // Trading commission
    BigDecimal brokerageFee;          // Brokerage fee
    BigDecimal exchangeFee;            // Exchange fee
    BigDecimal taxAmount;              // Applicable tax
    
    // Risk Checks
    Boolean riskCheckPassed;           // Risk validation
    Boolean buyingPowerCheck;          // Buying power validation
    Boolean positionLimitCheck;        // Position limit validation
    
    // Timestamps
    ZonedDateTime submittedAt;         // Submission time
    ZonedDateTime acceptedAt;          // Acceptance time
    ZonedDateTime firstFillAt;         // First execution time
    ZonedDateTime lastFillAt;          // Last execution time
    ZonedDateTime completedAt;         // Completion time
    
    // Execution History
    List<OrderExecution> executions;   // All executions
    
    // Advanced Features
    String strategyId;                 // Strategy identifier
    String algoId;                     // Algorithm identifier
    String parentOrderId;              // Parent order (for child orders)
    List<Order> childOrders;           // Child orders (bracket/OCO)
}
```

### 1.4 Order Service Methods

**File:** `/src/main/java/com/bisttrading/trading/service/OrderManagementService.java`

```java
@Service
public class OrderManagementService {
    
    /**
     * Create and place a new order
     * Flow: Validate → Check Symbol → Save to DB → Send to AlgoLab → Update Status
     */
    @Transactional
    public Order createOrder(CreateOrderRequest request, String userId) {
        // Returns: Order entity with status PENDING/SUBMITTED/REJECTED
    }
    
    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(String orderId) {
        // Returns: Order if found
    }
    
    /**
     * Get user's order history with pagination
     * Supports filtering by status, symbol, date range, etc.
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrderHistory(String userId, OrderSearchCriteria criteria, Pageable pageable) {
        // Returns: Page<Order> with total elements
        // Example: Page 0, Size 20, Sorted by created_at DESC
    }
    
    /**
     * Get active orders (PENDING, SUBMITTED, ACCEPTED, PARTIALLY_FILLED)
     */
    @Transactional(readOnly = true)
    public List<Order> getActiveOrders(String userId) {
        // Returns: List of orders still open
    }
    
    /**
     * Cancel an order
     * Sends cancellation to AlgoLab and updates status to CANCELLED
     */
    @Transactional
    public Order cancelOrder(String orderId) {
        // Returns: Updated order with status CANCELLED
        // Throws: OrderNotFoundException, OrderNotCancellableException
    }
    
    /**
     * Modify order price and/or quantity
     */
    @Transactional
    public Order modifyOrder(String orderId, BigDecimal newPrice, Integer newQuantity) {
        // Returns: Updated order with status REPLACED
    }
    
    /**
     * Update order status and record execution
     * Called when AlgoLab sends execution updates
     */
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus, OrderExecutionDetails details) {
        // Returns: Updated order
        // Side effect: Creates OrderExecution record
    }
    
    /**
     * Sync order status from AlgoLab (scheduled every 30 seconds)
     */
    @Scheduled(fixedDelay = 30000)
    public void syncOrderStatusFromAlgoLab() {
        // Internal sync mechanism
    }
}
```

### 1.5 Order Statuses

**File:** `/src/main/java/com/bisttrading/entity/trading/enums/OrderStatus.java`

| Status | Code | Description | Can Cancel | Can Modify |
|--------|------|-------------|-----------|-----------|
| PENDING | PENDING | Created but not submitted | No | No |
| SUBMITTED | SUBMITTED | Sent to broker | Yes | Yes |
| ACCEPTED | ACCEPTED | Accepted by exchange | Yes | Yes |
| PARTIALLY_FILLED | PARTIALLY_FILLED | Partially executed | Yes | Yes |
| FILLED | FILLED | Completely filled | No | No |
| CANCELLED | CANCELLED | Cancelled by user/system | - | - |
| REJECTED | REJECTED | Rejected by broker | - | - |
| REPLACED | REPLACED | Replaced (modified) | - | - |
| EXPIRED | EXPIRED | Expired (GTD orders) | - | - |
| ERROR | ERROR | System error occurred | - | - |

### 1.6 Order Types and Their Requirements

**File:** `/src/main/java/com/bisttrading/entity/trading/enums/OrderType.java`

```java
MARKET              // Execute immediately at best price
LIMIT               // Execute at specified price or better (requires price)
STOP                // Becomes market when price hit (requires stopPrice)
STOP_LIMIT          // Becomes limit when price hit (requires both prices)
TRAILING_STOP       // Stop moves with favorable prices (requires stopPrice)
ICEBERG             // Large order with hidden quantity (requires price)
BRACKET             // Parent with profit target + stop loss
OCO                 // One-Cancels-Other (two orders, one executes)
```

### 1.7 Order Side

**File:** `/src/main/java/com/bisttrading/entity/trading/enums/OrderSide.java`

```java
BUY                 // Buy (long position)
SELL                // Sell (short/liquidation)

// Methods:
getOpposite()       // BUY → SELL, SELL → BUY
getMultiplier()     // BUY → +1, SELL → -1
```

### 1.8 Order Repository

**File:** `/src/main/java/com/bisttrading/repository/trading/OrderRepository.java`

```java
@Repository
public interface OrderRepository extends BaseRepository<Order, String> {
    
    // Find by IDs
    Optional<Order> findByClientOrderId(String clientOrderId);
    Optional<Order> findByBrokerOrderId(String brokerOrderId);
    Optional<Order> findByExchangeOrderId(String exchangeOrderId);
    
    // User orders
    Page<Order> findByUserId(String userId, Pageable pageable);
    List<Order> findByUserIdAndOrderStatus(String userId, OrderStatus status);
    List<Order> findByUserIdAndOrderStatusIn(String userId, List<OrderStatus> statuses);
    
    // Account orders
    Page<Order> findByAccountId(String accountId, Pageable pageable);
    List<Order> findActiveOrdersByAccountId(String accountId);
    
    // Symbol orders
    Page<Order> findBySymbolId(String symbolId, Pageable pageable);
    List<Order> findActiveOrdersBySymbolId(String symbolId);
    
    // Status queries
    List<Order> findByOrderStatusIn(List<OrderStatus> statuses);
    
    // Algo/Strategy orders
    List<Order> findByStrategyIdOrderById(String strategyId);
    List<Order> findByAlgoIdOrderById(String algoId);
}
```

---

## 2. Position Management (Pozisyon Yönetimi)

### 2.1 Position Entity

**File:** `/src/main/java/com/bisttrading/entity/trading/Position.java`

```java
@Entity
@Table(name = "positions")
public class Position extends BaseEntity {
    // Identification
    UserEntity user;                   // Position owner
    String accountId;                  // Broker account
    String subAccountId;               // Sub-account (optional)
    Symbol symbol;                     // Trading symbol
    
    // Position State
    PositionSide positionSide;         // LONG, SHORT, FLAT
    Integer quantity;                  // Net quantity (+ for long, - for short)
    Integer availableQuantity;         // Available for trading
    Integer blockedQuantity;           // Blocked by pending orders
    
    // Cost Basis
    BigDecimal averageCost;            // Cost per share
    BigDecimal totalCost;              // quantity * averageCost
    
    // P&L Information
    BigDecimal realizedPnl;            // Closed position P&L
    BigDecimal unrealizedPnl;          // Current position P&L
    
    // Market Information
    BigDecimal marketPrice;            // Current market price
    BigDecimal marketValue;            // quantity * marketPrice
    BigDecimal dayChange;              // Change from previous close
    BigDecimal dayChangePercent;       // Percentage change
    BigDecimal lastTradePrice;         // Last execution price
    ZonedDateTime lastTradeTime;       // Last execution time
    
    // Risk Metrics
    BigDecimal valueAtRisk;            // VaR calculation
    BigDecimal maximumDrawdown;        // Max drawdown since open
}
```

### 2.2 Position Operations

```java
@Entity
public class Position extends BaseEntity {
    
    // Query methods
    public boolean isFlat()             // quantity == 0 || FLAT
    public boolean isLong()             // side == LONG && quantity > 0
    public boolean isShort()            // side == SHORT && quantity < 0
    public Integer getAbsoluteQuantity()// Math.abs(quantity)
    
    // Update with trade execution
    public void updateWithTrade(OrderSide side, Integer quantity, BigDecimal price) {
        // Updates position based on trade
        // Calculates realized P&L if closing position
        // Updates position side if reversing
    }
    
    // Update market price (for real-time data)
    public void updateMarketPrice(BigDecimal newPrice) {
        // Recalculates unrealized P&L
        // Calculates day change
        // Updates market value
    }
    
    // Block/release quantity for orders
    public void blockQuantity(Integer quantity)    // For pending orders
    public void releaseQuantity(Integer quantity)  // When order cancels
    
    // P&L calculations
    public BigDecimal getTotalPnl()     // Realized + Unrealized
    public BigDecimal getPnlPercentage()// Total P&L / Total Cost * 100
    public BigDecimal getExposure()     // |marketValue|
    public BigDecimal getLeverage(BigDecimal marginUsed)
}
```

### 2.3 Position Sides

**File:** `/src/main/java/com/bisttrading/entity/trading/enums/PositionSide.java`

```java
LONG                // Own the security (quantity > 0)
SHORT               // Sold the security (quantity < 0)
FLAT                // No position (quantity == 0)
```

### 2.4 Position Repository

**File:** `/src/main/java/com/bisttrading/repository/trading/PositionRepository.java`

```java
@Repository
public interface PositionRepository extends BaseRepository<Position, String> {
    
    // By user
    List<Position> findByUserId(String userId);
    
    // By account
    List<Position> findByAccountId(String accountId);
    List<Position> findByUserIdAndAccountId(String userId, String accountId);
    
    // By symbol
    List<Position> findBySymbolId(String symbolId);
    Optional<Position> findByUserIdAndAccountIdAndSymbolId(
        String userId, String accountId, String symbolId);
    
    // By side
    List<Position> findByPositionSide(PositionSide side);
    List<Position> findByUserIdAndPositionSide(String userId, PositionSide side);
}
```

### 2.5 Get User Positions

To get all positions for a user:

```java
List<Position> positions = positionRepository.findByUserId(userId);

// Filter by account
List<Position> accountPositions = positionRepository
    .findByUserIdAndAccountId(userId, accountId);

// Get specific position
Optional<Position> position = positionRepository
    .findByUserIdAndAccountIdAndSymbolId(userId, accountId, symbol);

// Get only long/short positions
List<Position> longPositions = positionRepository
    .findByUserIdAndPositionSide(userId, PositionSide.LONG);
```

---

## 3. Pending Orders (Bekleyen Emirler)

### 3.1 Getting Pending Orders

Pending orders are queried through the `OrderManagementService`:

```java
// Get active orders (PENDING, SUBMITTED, ACCEPTED, PARTIALLY_FILLED)
List<Order> activeOrders = orderManagementService.getActiveOrders(userId);

// Or with detailed filtering
Page<Order> orders = orderManagementService.getOrderHistory(
    userId,
    OrderSearchCriteria.builder()
        .statuses(List.of(OrderStatus.SUBMITTED, OrderStatus.ACCEPTED))
        .build(),
    PageRequest.of(0, 20)
);
```

### 3.2 AlgoLab Pending Orders Integration

**File:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabOrderService.java`

```java
@Service
public class AlgoLabOrderService {
    
    /**
     * Get pending orders from AlgoLab
     * Uses TodaysTransaction API and filters for WAITING status
     */
    public List<Map<String, Object>> getPendingOrders(String subAccount) {
        // Flow:
        // 1. Call TodaysTransaction API
        // 2. Extract 'content' field (list of transactions)
        // 3. Filter for equityStatusDescription == "WAITING"
        // 4. Return list of pending orders
        
        // AlgoLab Response Structure:
        // {
        //   "success": true,
        //   "content": [
        //     {
        //       "orderId": "123",
        //       "symbol": "AKBNK",
        //       "direction": "BUY",
        //       "equityStatusDescription": "WAITING",
        //       "pricetype": "limit",
        //       "price": "15.75",
        //       "lot": "10",
        //       ...
        //     }
        //   ]
        // }
    }
}
```

### 3.3 Order Statuses Reference

| Status | In AlgoLab | Description |
|--------|-----------|-------------|
| WAITING | equityStatusDescription | Order pending execution |
| DONE | - | Order filled |
| DELETED | - | Order cancelled |
| KISMEN | - | Partially filled |

---

## 4. AlgoLab Integration

### 4.1 Authentication Flow

**Two-step authentication process:**

#### Step 1: Login with credentials (triggers SMS)

```java
@PostMapping("/api/v1/broker/auth/login")
@PreAuthorize("isAuthenticated()")  // Requires BIST user JWT
public ResponseEntity<BrokerAuthResponse> login(
    @Valid @RequestBody BrokerLoginRequest request,
    Authentication authentication
) {
    // Request:
    // {
    //   "username": "algolab_username",
    //   "password": "algolab_password"
    // }
    
    // Response:
    // {
    //   "success": true,
    //   "message": "SMS kodu telefonunuza gönderildi",
    //   "smsSent": true,
    //   "authenticated": false,
    //   "timestamp": "2025-10-15T10:30:00Z"
    // }
    
    String token = algoLabAuthService.loginUser(
        request.getUsername(),
        request.getPassword()
    );
    return ResponseEntity.ok(BrokerAuthResponse.loginSuccess());
}
```

#### Step 2: Verify SMS OTP

```java
@PostMapping("/api/v1/broker/auth/verify-otp")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<BrokerAuthResponse> verifyOtp(
    @Valid @RequestBody VerifyOtpRequest request,
    Authentication authentication
) {
    // Request:
    // {
    //   "otpCode": "123456"
    // }
    
    // Response (on success):
    // {
    //   "success": true,
    //   "message": "Doğrulama başarılı",
    //   "authenticated": true,
    //   "sessionExpiresAt": "2025-10-16T10:30:00Z",
    //   "timestamp": "2025-10-15T10:30:00Z"
    // }
    
    String hash = algoLabAuthService.loginUserControl(request.getOtpCode());
    return ResponseEntity.ok(BrokerAuthResponse.verifySuccess(expiresAt));
}
```

### 4.2 AlgoLab Session Management

**File:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabSessionManager.java`

```java
@Service
public class AlgoLabSessionManager {
    
    /**
     * Save session (token + hash)
     * Supports both file and database storage
     */
    @Transactional
    public void saveSession(String token, String hash) {
        // Saves to storage (file or database)
        // Default: 24-hour expiration
    }
    
    /**
     * Load session from persistent storage
     */
    @Transactional(readOnly = true)
    public AlgoLabSession loadSession() {
        // Returns: AlgoLabSession with token and hash
        // Or null if not found/expired
    }
    
    /**
     * Clear session on logout
     */
    @Transactional
    public void clearSession() {
        // Deactivates session in storage
    }
    
    /**
     * Update WebSocket connection status
     */
    @Transactional
    public void updateWebSocketStatus(boolean connected) {
        // Updates session with WebSocket state
    }
}

// Session Model
@Data
@Builder
public class AlgoLabSession {
    String token;                       // Authentication token
    String hash;                        // Authorization hash
    LocalDateTime lastUpdate;           // Last update time
    boolean websocketConnected;         // WebSocket status
    LocalDateTime websocketLastConnected;
}
```

**Storage Options:**

```yaml
algolab:
  session:
    storage: "database"               # Or "file"
    file-path: "./algolab-session.json"
    expiration-hours: 24
    retention-days: 30
```

### 4.3 AlgoLab API Calls

**File:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabOrderService.java`

#### Send Order

```java
public Map<String, Object> sendOrder(
    String symbol,          // "AKBNK"
    String direction,       // "BUY" or "SELL"
    String priceType,       // "limit" or "piyasa"
    BigDecimal price,       // Limit price (null for market)
    Integer lot,            // 1 lot = 100 shares
    Boolean sms,            // SMS notification
    Boolean email,          // Email notification
    String subAccount       // Sub-account ID (optional)
)

// Response:
// {
//   "success": true,
//   "content": {
//     "orderId": "ALG-123456",
//     "symbol": "AKBNK",
//     "direction": "BUY",
//     "status": "ACCEPTED"
//   },
//   "message": "Order sent successfully"
// }
```

#### Delete Order (Cancel)

```java
public Map<String, Object> deleteOrder(String orderId, String subAccount)

// Response:
// {
//   "success": true,
//   "message": "Order cancelled successfully"
// }
```

#### Modify Order

```java
public Map<String, Object> modifyOrder(
    String orderId,        // Order ID to modify
    BigDecimal price,      // New price
    Integer lot,           // New lot quantity
    Boolean viop,          // VIOP flag
    String subAccount
)

// Response:
// {
//   "success": true,
//   "content": {
//     "orderId": "ALG-123456",
//     "price": "16.00",
//     "lot": "15"
//   }
// }
```

#### Get Instant Position (Portfolio)

```java
public Map<String, Object> getInstantPosition(String subAccount)

// Response:
// {
//   "success": true,
//   "content": [
//     {
//       "symbol": "AKBNK",
//       "quantity": "1000",
//       "averagePrice": "15.73",
//       "marketPrice": "15.85",
//       "pnl": "+120.00"
//     }
//   ]
// }
```

#### Get Today's Transactions

```java
public Map<String, Object> getTodaysTransactions(String subAccount)

// Response includes all orders: pending, executed, cancelled
// Used by getPendingOrders() for filtering
```

#### Get Sub-Accounts

```java
public Map<String, Object> getSubAccounts()

// Response:
// {
//   "success": true,
//   "content": [
//     { "id": "ACC-001", "name": "Main Account" },
//     { "id": "ACC-002", "name": "Secondary Account" }
//   ]
// }
```

### 4.4 REST Client Configuration

**File:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabRestClient.java`

```java
@Component
public class AlgoLabRestClient {
    
    /**
     * Makes authenticated POST requests with resilience patterns:
     * - Circuit Breaker: Fails fast if AlgoLab unavailable
     * - Retry: Retries with exponential backoff
     * - Rate Limiting: 1 request per 5 seconds
     * - Timeout: Request-level timeout
     */
    public <T> ResponseEntity<T> post(
        String endpoint,        // "/api/SendOrder"
        Object payload,
        boolean authenticated,  // Add auth headers if true
        Class<T> responseType
    )
    
    // Rate Limit: 0.2 permits/sec = 1 request per 5 seconds
    // Metrics: Tracked with Micrometer
}
```

### 4.5 Authentication Service

**File:** `/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabAuthService.java`

```java
@Service
public class AlgoLabAuthService {
    
    /**
     * Step 1: Login with AlgoLab credentials
     */
    public String loginUser(String username, String password) {
        // Encrypts credentials
        // Calls /api/LoginUser
        // Returns token (needed for OTP verification)
    }
    
    /**
     * Step 2: Verify SMS OTP code
     */
    public String loginUserControl(String smsCode) {
        // Requires token from loginUser()
        // Calls /api/LoginUserControl with encrypted token + SMS code
        // Returns hash (authorization token)
        // Publishes AuthenticationCompletedEvent for WebSocket
    }
    
    /**
     * Restore session from saved token + hash
     */
    public boolean restoreSession() {
        // Loads session from storage
        // Validates with GetSubAccounts
        // Returns true if still valid
    }
    
    /**
     * Check if session is alive
     */
    public boolean isAlive() {
        // Calls GetSubAccounts to validate
        // Returns true if successful
    }
    
    /**
     * Periodic session refresh (keep-alive)
     */
    @Scheduled(fixedDelayString = "${algolab.auth.refresh-interval-ms:300000}")
    public void sessionRefresh() {
        // Calls /api/SessionRefresh every 5 minutes
    }
    
    /**
     * Clear authentication
     */
    public void clearAuth() {
        // Clears token and hash
        // Clears session storage
    }
}
```

---

## 5. Session Management

### 5.1 JWT Token Management (BIST Platform)

**File:** `/src/main/java/com/bisttrading/security/jwt/JwtTokenProvider.java`

- **Token Type:** JWT (JSON Web Token)
- **Expiration:** Configurable (default: 24 hours)
- **Claims:** userId, username, authorities, email, etc.
- **Header:** `Authorization: Bearer {jwt_token}`
- **Refresh:** Via `/api/v1/auth/refresh-token` endpoint

### 5.2 AlgoLab Session Persistence

**Storage Options:**

1. **File Storage** (Default for development)
   ```json
   {
     "token": "encrypted_token_from_algolab",
     "hash": "authorization_hash_from_algolab",
     "lastUpdate": "2025-10-23T10:30:00",
     "websocketConnected": true,
     "websocketLastConnected": "2025-10-23T10:25:00"
   }
   ```

2. **Database Storage** (For production)
   - Table: `algolab_sessions`
   - Supports per-user sessions
   - Automatic cleanup of expired sessions
   - Audit trail (created_at, expires_at, deactivated_at, etc.)

### 5.3 Session Lifecycle

```
1. User Login (BIST) → JWT Token issued
   ↓
2. User calls /api/v1/broker/auth/login → SMS sent to AlgoLab
   ↓
3. User calls /api/v1/broker/auth/verify-otp → Token + Hash saved
   ↓
4. Session valid for 24 hours (configurable)
   ↓
5. Automatic refresh every 5 minutes (keep-alive)
   ↓
6. Session expires after 24 hours → User must re-login
   ↓
7. Logout clears token + hash from storage
```

### 5.4 Session Entity (Database)

**File:** `/src/main/java/com/bisttrading/broker/algolab/entity/AlgoLabSessionEntity.java`

```java
@Entity
@Table(name = "algolab_sessions")
public class AlgoLabSessionEntity extends BaseEntity {
    UUID userId;                        // User reference
    String token;                       // AlgoLab token
    String hash;                        // AlgoLab authorization hash
    LocalDateTime expiresAt;            // Session expiration
    LocalDateTime lastRefreshAt;        // Last refresh time
    boolean websocketConnected;         // WebSocket status
    LocalDateTime websocketLastConnected;
    
    boolean active;                     // Is session active?
    String deactivationReason;          // Why was it deactivated?
    LocalDateTime deactivatedAt;        // When deactivated?
}
```

### 5.5 Session Endpoints

```bash
# Check authentication status
GET /api/v1/broker/auth/status
Authorization: Bearer {jwt_token}

Response:
{
  "authenticated": true,
  "sessionActive": true,
  "connectionAlive": true,
  "lastRefreshTime": "2025-10-15T10:25:00Z",
  "sessionExpiresAt": "2025-10-16T10:30:00Z",
  "websocketConnected": true,
  "message": "AlgoLab bağlantısı aktif"
}

# Logout
POST /api/v1/broker/auth/logout
Authorization: Bearer {jwt_token}

Response:
{
  "success": true,
  "message": "AlgoLab oturumu başarıyla sonlandırıldı",
  "authenticated": false
}
```

---

## 6. Implementation Guide for Telegram Bot

### 6.1 Required Information to Collect

```
1. BIST Trading Platform Account
   - Username/Email
   - Password
   → Get JWT token via /api/v1/auth/login

2. AlgoLab Broker Credentials
   - AlgoLab username
   - AlgoLab password
   → User authenticates via /api/v1/broker/auth/login (SMS sent)
   → User verifies SMS via /api/v1/broker/auth/verify-otp

3. Trading Account Info
   - Account ID from broker
   - Sub-account ID (if applicable)
```

### 6.2 Workflow Steps

```
[Telegram Bot User]
    ↓
[1. Login to BIST Platform] → /api/v1/auth/login → Get JWT
    ↓
[2. Initiate AlgoLab Auth] → /api/v1/broker/auth/login → SMS sent
    ↓
[3. Verify SMS Code] → /api/v1/broker/auth/verify-otp → Session created
    ↓
[4. Place Order] → /api/v1/broker/orders (POST)
    ↓
[5. View Active Orders] → /api/v1/broker/orders/active
    ↓
[6. View Positions] → /api/v1/broker/positions
    ↓
[7. Cancel Order] → /api/v1/broker/orders/{id}/cancel
    ↓
[8. Check Status] → /api/v1/broker/auth/status
```

### 6.3 API Call Examples

#### Example 1: Create Order

```bash
curl -X POST http://localhost:8080/api/v1/broker/orders \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AKBNK",
    "side": "BUY",
    "orderType": "LIMIT",
    "quantity": 1000,
    "price": 15.75,
    "timeInForce": "DAY",
    "accountId": "ACC-001",
    "smsNotification": false,
    "emailNotification": true
  }'

Response:
{
  "id": "ORD-123456",
  "symbol": "AKBNK",
  "side": "BUY",
  "orderType": "LIMIT",
  "quantity": 1000,
  "price": 15.75,
  "orderStatus": "SUBMITTED",
  "brokerOrderId": "ALG-987654321",
  "createdAt": "2025-10-15T10:30:00Z",
  "submittedAt": "2025-10-15T10:30:01Z"
}
```

#### Example 2: Get Active Orders

```bash
curl -X GET "http://localhost:8080/api/v1/broker/orders/active" \
  -H "Authorization: Bearer {jwt_token}"

Response:
{
  "content": [
    {
      "id": "ORD-123456",
      "symbol": "AKBNK",
      "side": "BUY",
      "quantity": 1000,
      "filledQuantity": 500,
      "remainingQuantity": 500,
      "price": 15.75,
      "averageFillPrice": 15.73,
      "orderStatus": "PARTIALLY_FILLED",
      "createdAt": "2025-10-15T10:30:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 1
}
```

#### Example 3: Get Positions

```bash
curl -X GET "http://localhost:8080/api/v1/broker/positions" \
  -H "Authorization: Bearer {jwt_token}"

Response:
{
  "content": [
    {
      "id": "POS-123",
      "symbol": "AKBNK",
      "quantity": 1500,
      "positionSide": "LONG",
      "averageCost": 15.70,
      "marketPrice": 15.85,
      "marketValue": 23775.00,
      "unrealizedPnl": 225.00,
      "realizedPnl": 50.00,
      "totalPnl": 275.00,
      "dayChange": 150.00,
      "dayChangePercent": 0.6347
    }
  ]
}
```

#### Example 4: Cancel Order

```bash
curl -X DELETE "http://localhost:8080/api/v1/broker/orders/ORD-123456" \
  -H "Authorization: Bearer {jwt_token}"

Response:
{
  "id": "ORD-123456",
  "symbol": "AKBNK",
  "orderStatus": "CANCELLED",
  "statusReason": "Cancelled by user",
  "completedAt": "2025-10-15T10:35:00Z"
}
```

---

## 7. API Endpoints Reference

### Authentication

```
POST   /api/v1/auth/login                    # BIST platform login
POST   /api/v1/auth/refresh-token            # Refresh JWT
POST   /api/v1/auth/logout                   # BIST logout

POST   /api/v1/broker/auth/login             # AlgoLab login (SMS)
POST   /api/v1/broker/auth/verify-otp        # Verify SMS code
GET    /api/v1/broker/auth/status            # Check connection status
POST   /api/v1/broker/auth/logout            # Logout from AlgoLab
```

### Orders

```
POST   /api/v1/broker/orders                 # Create order
GET    /api/v1/broker/orders                 # Get order history
GET    /api/v1/broker/orders/active          # Get active orders
GET    /api/v1/broker/orders/{id}            # Get order details
DELETE /api/v1/broker/orders/{id}            # Cancel order
PUT    /api/v1/broker/orders/{id}            # Modify order
```

### Positions

```
GET    /api/v1/broker/positions              # Get all positions
GET    /api/v1/broker/positions/{id}         # Get position details
GET    /api/v1/broker/positions/account/{accountId}  # By account
GET    /api/v1/broker/positions/symbol/{symbol}     # By symbol
```

### Market Data

```
GET    /api/v1/market/symbols                # Get available symbols
GET    /api/v1/market/symbols/{symbol}       # Get symbol details
GET    /api/v1/market/data/{symbol}/tick     # Get tick data
```

---

## 8. Key Technical Notes for Integration

### 8.1 Authentication Header

All API calls require JWT token:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 8.2 Error Handling

```java
// Common HTTP Status Codes
200  OK                          // Success
201  CREATED                     // Order created
400  BAD REQUEST                 // Validation failed
401  UNAUTHORIZED                // No/invalid JWT
402  PAYMENT REQUIRED            // Insufficient funds
403  FORBIDDEN                   // No permission
404  NOT FOUND                   // Order/position not found
428  PRECONDITION REQUIRED       // SMS not verified (need /login first)
429  TOO MANY REQUESTS           // Rate limit exceeded
500  INTERNAL SERVER ERROR       // Server error
503  SERVICE UNAVAILABLE         // AlgoLab down
```

### 8.3 Pagination

Most list endpoints support pagination:
```
GET /api/v1/broker/orders?page=0&size=20&sort=createdAt,desc

Response:
{
  "content": [...],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5,
  "hasMore": true
}
```

### 8.4 Required Permissions

```
trading:view       # View orders and positions
trading:place      # Place new orders
trading:cancel     # Cancel orders
trading:modify     # Modify orders
broker:auth        # Access broker authentication
```

### 8.5 Rate Limiting

- API: 1 request per 5 seconds per user (AlgoLab rate limit)
- Response: `429 Too Many Requests` when exceeded
- Consider caching frequently accessed data

### 8.6 Quantity Units

- **Quantity in Orders:** Number of shares (e.g., 1000 shares)
- **Lots in AlgoLab:** 1 lot = 100 shares
- **Conversion:** lots = quantity / 100

### 8.7 Price Format

- **Precision:** 6 decimal places (BigDecimal)
- **Min Value:** 0.01
- **Type:** BigDecimal (not float/double) for accuracy

### 8.8 Timestamps

- **Format:** ISO 8601 with timezone (ZonedDateTime)
- **Example:** 2025-10-15T10:30:00.000Z
- **Always UTC:** Convert to local time on client side

---

## Summary of Key Files

| Component | File | Purpose |
|-----------|------|---------|
| Order DTO | `trading/dto/CreateOrderRequest.java` | Order creation request |
| Order Entity | `entity/trading/Order.java` | Order persistence model |
| Order Service | `trading/service/OrderManagementService.java` | Order business logic |
| Position Entity | `entity/trading/Position.java` | Position persistence model |
| Order Statuses | `entity/trading/enums/OrderStatus.java` | Status lifecycle |
| Order Types | `entity/trading/enums/OrderType.java` | Order type definitions |
| AlgoLab Auth | `broker/algolab/service/AlgoLabAuthService.java` | Authentication flow |
| AlgoLab Orders | `broker/algolab/service/AlgoLabOrderService.java` | Order API calls |
| AlgoLab Session | `broker/algolab/service/AlgoLabSessionManager.java` | Session persistence |
| REST Client | `broker/algolab/service/AlgoLabRestClient.java` | HTTP client wrapper |
| Auth Controller | `broker/controller/BrokerAuthController.java` | Auth endpoints |
| Broker Controller | `broker/controller/BrokerController.java` | Trading endpoints |

---

## Contact & Support

For issues or questions about the trading implementation:
1. Check OpenAPI documentation at `/swagger-ui.html`
2. Review error messages and logs
3. Validate request formats against DTOs
4. Ensure AlgoLab session is authenticated before trading

**Version 2.0.0** - Production Ready
**Last Updated:** October 23, 2025


# Telegram Bot Implementation - Documentation Index

**Project:** BIST Trading Platform  
**Version:** 2.0.0  
**Generated:** October 23, 2025  
**Status:** Production Ready  

---

## Overview

This directory contains comprehensive documentation for implementing the trading functionality in a Telegram bot. All necessary information about order management, positions, pending orders, AlgoLab integration, and session management has been extracted and documented.

---

## Documentation Files

### 1. EXPLORATION_SUMMARY.md (28 KB)
**Quick Reference & Implementation Checklist**

Use this document for:
- Executive summary of all trading functionality
- Quick lookup of key methods and parameters
- Implementation checklist for Telegram bot
- File listings with line numbers
- API endpoint reference
- Error handling patterns
- Constants and conversions

**Key Sections:**
- Order Management overview
- Position Management overview
- AlgoLab Integration methods
- Session Management details
- Enum reference
- Production deployment notes

**When to Use:** During Telegram bot implementation as a quick reference

---

### 2. TRADING_FUNCTIONALITY_GUIDE.md (35 KB)
**Comprehensive Technical Reference**

Use this document for:
- Detailed explanations of all trading concepts
- Complete DTO and entity structures
- Service method signatures and descriptions
- Repository query methods
- Authentication flow details
- API endpoint specifications
- Implementation examples with curl commands
- Workflow steps for Telegram bot

**Key Sections:**
- Order Management (1 Order Lifecycle through 1.8 Order Repository)
- Position Management (2.1 Position Entity through 2.5 Get User Positions)
- Pending Orders (3.1 Getting Pending Orders through 3.3 Order Statuses)
- AlgoLab Integration (4.1 Authentication Flow through 4.5 Authentication Service)
- Session Management (5.1 JWT Token through 5.5 Session Endpoints)
- Implementation Guide (6.1 Required Information through 6.3 API Call Examples)
- API Endpoints (7 Complete Reference)

**When to Use:** For deep understanding and detailed implementation guidance

---

## Quick Start for Telegram Bot Implementation

### Step 1: Understand the Architecture
Read sections in this order:
1. EXPLORATION_SUMMARY.md - "Executive Summary"
2. TRADING_FUNCTIONALITY_GUIDE.md - "Overview"

**Time:** 5-10 minutes

### Step 2: Understand Authentication
Read:
1. TRADING_FUNCTIONALITY_GUIDE.md - "5. Session Management" (5.1-5.3)
2. TRADING_FUNCTIONALITY_GUIDE.md - "4.1 Authentication Flow"

**Time:** 15-20 minutes

### Step 3: Understand Orders
Read:
1. EXPLORATION_SUMMARY.md - "1. ORDER MANAGEMENT"
2. TRADING_FUNCTIONALITY_GUIDE.md - "1. Order Management" (1.1-1.8)

**Time:** 20-30 minutes

### Step 4: Understand Positions
Read:
1. EXPLORATION_SUMMARY.md - "2. POSITION MANAGEMENT"
2. TRADING_FUNCTIONALITY_GUIDE.md - "2. Position Management" (2.1-2.5)

**Time:** 10-15 minutes

### Step 5: Understand Pending Orders
Read:
1. EXPLORATION_SUMMARY.md - "3. PENDING ORDERS"
2. TRADING_FUNCTIONALITY_GUIDE.md - "3. Pending Orders" (3.1-3.3)

**Time:** 5-10 minutes

### Step 6: Understand AlgoLab Integration
Read:
1. EXPLORATION_SUMMARY.md - "4. ALGOLAB INTEGRATION"
2. TRADING_FUNCTIONALITY_GUIDE.md - "4. AlgoLab Integration" (4.1-4.5)

**Time:** 25-30 minutes

### Step 7: Implementation & Testing
Read:
1. TRADING_FUNCTIONALITY_GUIDE.md - "6.3 API Call Examples"
2. EXPLORATION_SUMMARY.md - "9. IMPLEMENTATION CHECKLIST"
3. TRADING_FUNCTIONALITY_GUIDE.md - "7. API Endpoints Reference"

**Time:** 20-30 minutes

---

## Key File Paths (Absolute)

### Core Trading Files
```
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/trading/dto/CreateOrderRequest.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/Order.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/Position.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/trading/service/OrderManagementService.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/repository/trading/OrderRepository.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/repository/trading/PositionRepository.java
```

### AlgoLab Integration Files
```
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabAuthService.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabOrderService.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabSessionManager.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/algolab/service/AlgoLabRestClient.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/controller/BrokerAuthController.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/broker/controller/BrokerController.java
```

### Enum Files
```
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/enums/OrderStatus.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/enums/OrderType.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/enums/OrderSide.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/enums/PositionSide.java
/Users/onurerdogan/dev/bist-trading-platform/src/main/java/com/bisttrading/entity/trading/enums/TimeInForce.java
```

---

## API Endpoints by Category

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
GET    /api/v1/broker/orders                 # Order history (paginated)
GET    /api/v1/broker/orders/active          # Get active orders
GET    /api/v1/broker/orders/{id}            # Get order details
DELETE /api/v1/broker/orders/{id}            # Cancel order
PUT    /api/v1/broker/orders/{id}            # Modify order
```

### Positions
```
GET    /api/v1/broker/positions              # All positions (paginated)
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

## Important Constants

### Quantity Conversions
- 1 lot = 100 shares
- Platform uses shares, AlgoLab uses lots
- Formula: lots = quantity / 100

### Price Precision
- Type: BigDecimal (NOT float/double)
- Decimal places: 6
- Example: 15.750000

### Rate Limiting
- Limit: 1 request per 5 seconds per user
- Implementation: RateLimiter with 0.2 permits/second
- Error: 429 Too Many Requests

### Session Management
- JWT Expiration: 24 hours (configurable)
- AlgoLab Token: 24 hours (configurable)
- Auto-refresh: Every 5 minutes

---

## Order Statuses

| Status | Description | Can Cancel | Can Modify |
|--------|------------|-----------|-----------|
| PENDING | Created but not submitted | No | No |
| SUBMITTED | Sent to broker | Yes | Yes |
| ACCEPTED | Accepted by exchange | Yes | Yes |
| PARTIALLY_FILLED | Partially executed | Yes | Yes |
| FILLED | Completely filled | No | No |
| CANCELLED | Cancelled by user/system | - | - |
| REJECTED | Rejected by broker | - | - |
| REPLACED | Modified (replaced) | - | - |
| SUSPENDED | Suspended | No | No |
| EXPIRED | Expired | - | - |
| ERROR | System error | - | - |

---

## Order Types

```
MARKET           Execute immediately at best price
LIMIT            Execute at specific price (requires price)
STOP             Becomes market at trigger (requires stopPrice)
STOP_LIMIT       Becomes limit at trigger (requires both prices)
TRAILING_STOP    Stop moves with price (requires stopPrice)
ICEBERG          Large hidden order (requires price)
BRACKET          Parent with take-profit + stop-loss
OCO              One-Cancels-Other (two orders)
```

---

## Implementation Checklist

### User Information Collection
- [ ] BIST Trading Platform username/email
- [ ] BIST Trading Platform password
- [ ] AlgoLab broker username
- [ ] AlgoLab broker password
- [ ] Broker account ID
- [ ] Sub-account ID (optional)

### Authentication
- [ ] Step 1: `/api/v1/auth/login` → Get JWT
- [ ] Step 2: `/api/v1/broker/auth/login` → Trigger SMS
- [ ] Step 3: Collect SMS code from user
- [ ] Step 4: `/api/v1/broker/auth/verify-otp` → Session created

### Trading Features
- [ ] Place order
- [ ] View active orders
- [ ] View order history
- [ ] Cancel order
- [ ] Modify order
- [ ] View positions
- [ ] Check connection status
- [ ] Handle rate limiting

### Error Handling
- [ ] 401 Unauthorized
- [ ] 402 Insufficient funds
- [ ] 403 Forbidden
- [ ] 404 Not found
- [ ] 428 Precondition required (SMS not verified)
- [ ] 429 Rate limit exceeded
- [ ] 500/503 Service errors

---

## HTTP Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Process response |
| 201 | Created | Order placed successfully |
| 400 | Bad Request | Validate input |
| 401 | Unauthorized | Refresh JWT or re-authenticate |
| 402 | Insufficient Funds | Check account balance |
| 403 | Forbidden | Check permissions |
| 404 | Not Found | Verify ID |
| 428 | Precondition Required | Call /login first |
| 429 | Rate Limited | Wait 5 seconds before retry |
| 500 | Server Error | Report to admin |
| 503 | Service Down | AlgoLab unavailable |

---

## Testing the API

### Using curl
```bash
# Login to BIST
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user@example.com","password":"pass"}'

# Place order
curl -X POST http://localhost:8080/api/v1/broker/orders \
  -H "Authorization: Bearer {jwt_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol":"AKBNK",
    "side":"BUY",
    "orderType":"LIMIT",
    "quantity":1000,
    "price":15.75,
    "accountId":"ACC-001"
  }'
```

### Using Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

## Configuration

### For Development
```yaml
algolab:
  session:
    storage: "file"
    file-path: "./algolab-session.json"
    expiration-hours: 24
```

### For Production
```yaml
algolab:
  session:
    storage: "database"
    expiration-hours: 24
    retention-days: 90
  
  api:
    rate-limit: 0.2
    timeout-ms: 30000
  
  auth:
    keep-alive: true
    refresh-interval-ms: 300000
```

---

## Troubleshooting

### Common Issues

1. **428 Precondition Required**
   - Cause: SMS not verified
   - Solution: Call `/broker/auth/login` first, then `/verify-otp`

2. **429 Too Many Requests**
   - Cause: Exceeding rate limit (1 per 5 seconds)
   - Solution: Implement queue, retry after 5 seconds

3. **401 Unauthorized**
   - Cause: Invalid JWT token
   - Solution: Refresh token or re-authenticate

4. **402 Insufficient Funds**
   - Cause: Not enough balance
   - Solution: Check account balance, reduce order size

5. **503 Service Down**
   - Cause: AlgoLab unavailable
   - Solution: Retry later, implement circuit breaker

---

## Support Resources

1. **API Documentation:** `/swagger-ui.html` (after starting backend)
2. **Error Messages:** Check response body for error description
3. **Logs:** Check backend logs in `/logs` directory
4. **Test Data:** See algolab-session-dev.json for session structure

---

## Summary

This documentation package provides everything needed to implement trading functionality in a Telegram bot:

- **EXPLORATION_SUMMARY.md** - Quick reference (28 KB)
- **TRADING_FUNCTIONALITY_GUIDE.md** - Comprehensive guide (35 KB)
- **TELEGRAM_BOT_IMPLEMENTATION_INDEX.md** - This file (documentation index)

**Total Documentation:** ~65 KB of technical specifications, code examples, and implementation guidelines

---

**Generated:** October 23, 2025  
**Version:** 2.0.0  
**Status:** Production Ready  

For questions or clarifications, refer to the appropriate documentation file or check the source code in the referenced file paths.


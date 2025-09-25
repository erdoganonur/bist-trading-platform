# REST API Documentation - BIST Trading Platform

## Overview

The BIST Trading Platform exposes RESTful APIs for user management, market data access, and trading operations. All APIs follow REST principles with JSON request/response format and standard HTTP status codes.

## Base URLs

| Service | Port | Base URL | Description |
|---------|------|----------|-------------|
| **User Management** | 8081 | `http://localhost:8081/api` | Authentication and user operations |
| **Market Data** | 8082 | `http://localhost:8082/api` | Market data and analytics |
| **Broker Integration** | 8083 | `http://localhost:8083/api` | Trading and order management |

## Authentication

### JWT Token Authentication

All protected endpoints require a valid JWT token in the Authorization header:

```http
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
```

### Authentication Flow

1. **Login** → Receive JWT token
2. **Include token** in subsequent requests
3. **Refresh token** when expired (optional)

---

## User Management Service API (Port: 8081)

### Authentication Endpoints

#### POST /api/auth/login
**Description**: Authenticate user and receive JWT token

**Request**:
```json
{
  "username": "trader@example.com",
  "password": "SecurePassword123",
  "rememberMe": true
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "refresh_token_string",
    "expiresIn": 3600,
    "user": {
      "id": "USER-12345",
      "username": "trader@example.com",
      "firstName": "Ahmet",
      "lastName": "Yılmaz",
      "role": "TRADER",
      "lastLogin": "2024-09-24T10:30:00Z"
    }
  },
  "message": "Login successful"
}
```

**Response (401 Unauthorized)**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Invalid username or password",
    "details": "Authentication failed"
  },
  "timestamp": "2024-09-24T10:30:00Z"
}
```

#### POST /api/auth/logout
**Description**: Logout user and invalidate token

**Headers**: `Authorization: Bearer {token}`

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Logout successful"
}
```

#### POST /api/auth/refresh
**Description**: Refresh expired JWT token

**Request**:
```json
{
  "refreshToken": "refresh_token_string"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "token": "new_jwt_token",
    "expiresIn": 3600
  }
}
```

### User Profile Endpoints

#### GET /api/users/profile
**Description**: Get current user profile

**Headers**: `Authorization: Bearer {token}`

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": "USER-12345",
    "username": "trader@example.com",
    "firstName": "Ahmet",
    "lastName": "Yılmaz",
    "email": "trader@example.com",
    "phone": "+905551234567",
    "tckn": "12345678901",
    "preferences": {
      "language": "tr",
      "timezone": "Europe/Istanbul",
      "currency": "TRY"
    },
    "createdAt": "2024-01-15T08:00:00Z",
    "lastLogin": "2024-09-24T10:30:00Z"
  }
}
```

#### PUT /api/users/profile
**Description**: Update user profile

**Headers**: `Authorization: Bearer {token}`

**Request**:
```json
{
  "firstName": "Mehmet",
  "lastName": "Demir",
  "phone": "+905559876543",
  "preferences": {
    "language": "en",
    "timezone": "Europe/Istanbul",
    "currency": "TRY"
  }
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": "USER-12345",
    "firstName": "Mehmet",
    "lastName": "Demir",
    "phone": "+905559876543",
    "preferences": {
      "language": "en",
      "timezone": "Europe/Istanbul",
      "currency": "TRY"
    }
  },
  "message": "Profile updated successfully"
}
```

#### POST /api/users/change-password
**Description**: Change user password

**Headers**: `Authorization: Bearer {token}`

**Request**:
```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewPassword456",
  "confirmPassword": "NewPassword456"
}
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "Password changed successfully"
}
```

---

## Market Data Service API (Port: 8082)

### Market Data Endpoints

#### GET /api/market-data/symbols
**Description**: Get list of available Turkish stock symbols

**Query Parameters**:
- `exchange` (optional): Filter by exchange (e.g., "BIST")
- `sector` (optional): Filter by sector
- `limit` (optional): Number of results (default: 100)

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "symbols": [
      {
        "symbol": "AKBNK",
        "name": "Akbank T.A.Ş.",
        "exchange": "BIST",
        "sector": "Banking",
        "currency": "TRY",
        "isin": "TRAAKBNK91E6"
      },
      {
        "symbol": "THYAO",
        "name": "Türk Hava Yolları A.O.",
        "exchange": "BIST",
        "sector": "Transportation",
        "currency": "TRY",
        "isin": "TRATHYAO91K1"
      }
    ],
    "total": 150,
    "page": 1,
    "limit": 100
  }
}
```

#### GET /api/market-data/quotes/{symbol}
**Description**: Get real-time quote for a symbol

**Path Parameters**:
- `symbol`: Stock symbol (e.g., "AKBNK")

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "symbol": "AKBNK",
    "price": 15.75,
    "change": 0.25,
    "changePercent": 1.61,
    "volume": 1250000,
    "bidPrice": 15.74,
    "askPrice": 15.76,
    "bidSize": 5000,
    "askSize": 7500,
    "high": 15.85,
    "low": 15.50,
    "open": 15.60,
    "previousClose": 15.50,
    "timestamp": "2024-09-24T10:30:00Z",
    "marketStatus": "OPEN"
  }
}
```

#### GET /api/market-data/history/{symbol}
**Description**: Get historical price data

**Path Parameters**:
- `symbol`: Stock symbol

**Query Parameters**:
- `period`: Time period ("1d", "5d", "1m", "3m", "1y")
- `interval`: Data interval ("1m", "5m", "1h", "1d")

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "symbol": "AKBNK",
    "period": "1d",
    "interval": "5m",
    "data": [
      {
        "timestamp": "2024-09-24T09:30:00Z",
        "open": 15.60,
        "high": 15.65,
        "low": 15.58,
        "close": 15.62,
        "volume": 125000
      },
      {
        "timestamp": "2024-09-24T09:35:00Z",
        "open": 15.62,
        "high": 15.70,
        "low": 15.61,
        "close": 15.68,
        "volume": 98000
      }
    ]
  }
}
```

#### GET /api/market-data/watchlist
**Description**: Get user's watchlist with real-time quotes

**Headers**: `Authorization: Bearer {token}`

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "watchlist": [
      {
        "symbol": "AKBNK",
        "price": 15.75,
        "change": 0.25,
        "changePercent": 1.61,
        "volume": 1250000,
        "timestamp": "2024-09-24T10:30:00Z"
      },
      {
        "symbol": "THYAO",
        "price": 45.50,
        "change": -1.25,
        "changePercent": -2.67,
        "volume": 875000,
        "timestamp": "2024-09-24T10:30:00Z"
      }
    ]
  }
}
```

#### POST /api/market-data/watchlist
**Description**: Add symbol to watchlist

**Headers**: `Authorization: Bearer {token}`

**Request**:
```json
{
  "symbol": "GARAN"
}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "message": "Symbol added to watchlist",
  "data": {
    "symbol": "GARAN",
    "addedAt": "2024-09-24T10:30:00Z"
  }
}
```

---

## Broker Integration Service API (Port: 8083)

### Order Management Endpoints

#### POST /api/orders
**Description**: Place a new order

**Headers**: `Authorization: Bearer {token}`

**Request**:
```json
{
  "symbol": "AKBNK",
  "side": "BUY",
  "orderType": "LIMIT",
  "quantity": 1000,
  "price": 15.75,
  "timeInForce": "DAY",
  "subAccount": ""
}
```

**Response (201 Created)**:
```json
{
  "success": true,
  "data": {
    "orderId": "ORD-123456789",
    "brokerOrderId": "ALG-987654321",
    "symbol": "AKBNK",
    "side": "BUY",
    "orderType": "LIMIT",
    "quantity": 1000,
    "price": 15.75,
    "status": "SUBMITTED",
    "timestamp": "2024-09-24T10:30:00Z"
  },
  "message": "Order submitted successfully"
}
```

**Response (400 Bad Request)**:
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_BALANCE",
    "message": "Insufficient account balance for this order",
    "details": "Required: 15,750.00 TRY, Available: 10,000.00 TRY"
  }
}
```

#### GET /api/orders
**Description**: Get user's orders

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `status` (optional): Filter by status ("SUBMITTED", "FILLED", "CANCELLED")
- `symbol` (optional): Filter by symbol
- `limit` (optional): Number of results (default: 50)
- `offset` (optional): Pagination offset

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "orders": [
      {
        "orderId": "ORD-123456789",
        "brokerOrderId": "ALG-987654321",
        "symbol": "AKBNK",
        "side": "BUY",
        "orderType": "LIMIT",
        "quantity": 1000,
        "filledQuantity": 500,
        "price": 15.75,
        "averagePrice": 15.73,
        "status": "PARTIALLY_FILLED",
        "timestamp": "2024-09-24T10:30:00Z",
        "lastUpdate": "2024-09-24T10:35:00Z"
      }
    ],
    "total": 25,
    "page": 1,
    "limit": 50
  }
}
```

#### GET /api/orders/{orderId}
**Description**: Get specific order details

**Headers**: `Authorization: Bearer {token}`

**Path Parameters**:
- `orderId`: Order ID

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "orderId": "ORD-123456789",
    "brokerOrderId": "ALG-987654321",
    "symbol": "AKBNK",
    "side": "BUY",
    "orderType": "LIMIT",
    "quantity": 1000,
    "filledQuantity": 1000,
    "price": 15.75,
    "averagePrice": 15.74,
    "status": "FILLED",
    "commission": 7.87,
    "timestamp": "2024-09-24T10:30:00Z",
    "fills": [
      {
        "quantity": 500,
        "price": 15.73,
        "timestamp": "2024-09-24T10:32:00Z"
      },
      {
        "quantity": 500,
        "price": 15.75,
        "timestamp": "2024-09-24T10:35:00Z"
      }
    ]
  }
}
```

#### DELETE /api/orders/{orderId}
**Description**: Cancel an order

**Headers**: `Authorization: Bearer {token}`

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "orderId": "ORD-123456789",
    "status": "CANCELLED",
    "cancelledAt": "2024-09-24T10:45:00Z"
  },
  "message": "Order cancelled successfully"
}
```

### Portfolio Endpoints

#### GET /api/portfolio
**Description**: Get portfolio positions

**Headers**: `Authorization: Bearer {token}`

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "totalValue": 125750.50,
    "totalCost": 120000.00,
    "totalPnl": 5750.50,
    "totalPnlPercent": 4.79,
    "cashBalance": 25000.00,
    "positions": [
      {
        "symbol": "AKBNK",
        "quantity": 1000,
        "averagePrice": 15.50,
        "currentPrice": 15.75,
        "marketValue": 15750.00,
        "cost": 15500.00,
        "pnl": 250.00,
        "pnlPercent": 1.61
      },
      {
        "symbol": "THYAO",
        "quantity": 2000,
        "averagePrice": 47.25,
        "currentPrice": 45.50,
        "marketValue": 91000.00,
        "cost": 94500.00,
        "pnl": -3500.00,
        "pnlPercent": -3.70
      }
    ]
  }
}
```

#### GET /api/portfolio/transactions
**Description**: Get transaction history

**Headers**: `Authorization: Bearer {token}`

**Query Parameters**:
- `startDate` (optional): Start date (ISO format)
- `endDate` (optional): End date (ISO format)
- `symbol` (optional): Filter by symbol
- `type` (optional): Transaction type ("BUY", "SELL", "DIVIDEND")

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "transactions": [
      {
        "transactionId": "TXN-789123456",
        "orderId": "ORD-123456789",
        "symbol": "AKBNK",
        "type": "BUY",
        "quantity": 1000,
        "price": 15.75,
        "amount": 15750.00,
        "commission": 7.87,
        "netAmount": 15757.87,
        "timestamp": "2024-09-24T10:35:00Z"
      }
    ],
    "total": 15,
    "summary": {
      "totalBuyAmount": 95000.00,
      "totalSellAmount": 0.00,
      "totalCommissions": 47.50
    }
  }
}
```

## Common Response Format

### Success Response
```json
{
  "success": true,
  "data": { /* response data */ },
  "message": "Operation successful",
  "timestamp": "2024-09-24T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "User-friendly error message",
    "details": "Technical details about the error"
  },
  "timestamp": "2024-09-24T10:30:00Z"
}
```

## HTTP Status Codes

| Code | Description | Usage |
|------|-------------|--------|
| **200** | OK | Successful GET, PUT, DELETE |
| **201** | Created | Successful POST (resource created) |
| **400** | Bad Request | Invalid request data |
| **401** | Unauthorized | Invalid or missing authentication |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Resource not found |
| **409** | Conflict | Resource conflict (duplicate) |
| **422** | Unprocessable Entity | Validation errors |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Server error |
| **503** | Service Unavailable | Service temporarily unavailable |

## Error Codes

### Authentication Errors
- `INVALID_CREDENTIALS` - Wrong username/password
- `TOKEN_EXPIRED` - JWT token expired
- `TOKEN_INVALID` - Invalid JWT token
- `ACCOUNT_LOCKED` - Account temporarily locked

### Validation Errors
- `REQUIRED_FIELD` - Required field missing
- `INVALID_FORMAT` - Invalid data format
- `INVALID_SYMBOL` - Unknown stock symbol
- `INVALID_QUANTITY` - Invalid order quantity

### Trading Errors
- `INSUFFICIENT_BALANCE` - Not enough account balance
- `MARKET_CLOSED` - Market is closed
- `ORDER_NOT_FOUND` - Order not found
- `CANNOT_CANCEL` - Order cannot be cancelled

### System Errors
- `SYSTEM_ERROR` - Internal system error
- `SERVICE_UNAVAILABLE` - External service unavailable
- `RATE_LIMIT_EXCEEDED` - Too many requests

## Rate Limiting

All APIs are subject to rate limiting:

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| **Authentication** | 10 requests | 1 minute |
| **Market Data** | 100 requests | 1 minute |
| **Trading** | 50 requests | 1 minute |
| **General** | 200 requests | 1 minute |

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1695546660
```

## API Versioning

Current API version: `v1`

Version is specified in the URL path:
```
/api/v1/users/profile
```

## Testing

### Postman Collection
A Postman collection is available at: `docs/api/BIST-Trading-Platform.postman_collection.json`

### cURL Examples

**Login**:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "trader@example.com",
    "password": "password123"
  }'
```

**Get Market Quote**:
```bash
curl -X GET http://localhost:8082/api/market-data/quotes/AKBNK \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Place Order**:
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "symbol": "AKBNK",
    "side": "BUY",
    "orderType": "LIMIT",
    "quantity": 100,
    "price": 15.75
  }'
```

---

**Last Updated:** September 2024
**Version:** 1.0
**Maintainer:** BIST Trading Platform Team
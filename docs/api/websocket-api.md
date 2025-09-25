# WebSocket API Documentation - BIST Trading Platform

## Overview

The BIST Trading Platform provides real-time data streaming through WebSocket connections for market data, order updates, and system notifications. WebSocket APIs enable low-latency, bi-directional communication for time-sensitive trading operations.

## Connection URLs

| Service | WebSocket URL | Purpose |
|---------|---------------|---------|
| **Market Data** | `ws://localhost:8082/ws/market-data` | Real-time market data streaming |
| **Trading** | `ws://localhost:8083/ws/trading` | Order updates and notifications |
| **User Notifications** | `ws://localhost:8081/ws/notifications` | System and user notifications |

## Authentication

### Token-based Authentication

WebSocket connections require JWT token authentication either via:

1. **Query Parameter**: `?token=YOUR_JWT_TOKEN`
2. **Authorization Header**: During WebSocket handshake
3. **First Message**: Send authentication message after connection

#### Example Connection with Token
```javascript
const ws = new WebSocket('ws://localhost:8082/ws/market-data?token=eyJ0eXAiOiJKV1Q...');
```

#### Example Authentication Message
```javascript
ws.send(JSON.stringify({
  type: 'AUTH',
  token: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...'
}));
```

---

## Market Data WebSocket API (Port: 8082)

### Connection Endpoint
`ws://localhost:8082/ws/market-data`

### Message Types

#### 1. Authentication
**Client → Server**
```json
{
  "type": "AUTH",
  "token": "eyJ0eXAiOiJKV1Q..."
}
```

**Server → Client (Success)**
```json
{
  "type": "AUTH_SUCCESS",
  "message": "Authentication successful",
  "userId": "USER-12345",
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

**Server → Client (Error)**
```json
{
  "type": "AUTH_ERROR",
  "error": {
    "code": "INVALID_TOKEN",
    "message": "Invalid or expired token"
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### 2. Symbol Subscription
**Client → Server**
```json
{
  "type": "SUBSCRIBE",
  "symbols": ["AKBNK", "THYAO", "GARAN"],
  "dataTypes": ["QUOTE", "TRADE", "DEPTH"]
}
```

**Server → Client (Confirmation)**
```json
{
  "type": "SUBSCRIPTION_CONFIRMED",
  "symbols": ["AKBNK", "THYAO", "GARAN"],
  "dataTypes": ["QUOTE", "TRADE", "DEPTH"],
  "subscriptionId": "SUB-789123456",
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### 3. Real-time Quote Updates
**Server → Client**
```json
{
  "type": "QUOTE",
  "symbol": "AKBNK",
  "data": {
    "bidPrice": 15.74,
    "bidSize": 5000,
    "askPrice": 15.76,
    "askSize": 7500,
    "lastPrice": 15.75,
    "lastSize": 1000,
    "change": 0.25,
    "changePercent": 1.61,
    "volume": 1250000,
    "high": 15.85,
    "low": 15.50,
    "open": 15.60,
    "previousClose": 15.50,
    "vwap": 15.68,
    "marketValue": 19687500000.00
  },
  "timestamp": "2024-09-24T10:30:15.123Z"
}
```

#### 4. Trade Updates
**Server → Client**
```json
{
  "type": "TRADE",
  "symbol": "AKBNK",
  "data": {
    "tradeId": "TRD-123456789",
    "price": 15.75,
    "quantity": 1000,
    "side": "BUY",
    "timestamp": "2024-09-24T10:30:15.456Z",
    "conditions": ["REGULAR"]
  },
  "timestamp": "2024-09-24T10:30:15.456Z"
}
```

#### 5. Order Book Depth
**Server → Client**
```json
{
  "type": "DEPTH",
  "symbol": "AKBNK",
  "data": {
    "bids": [
      {"price": 15.74, "size": 5000, "orders": 12},
      {"price": 15.73, "size": 8500, "orders": 18},
      {"price": 15.72, "size": 12000, "orders": 25}
    ],
    "asks": [
      {"price": 15.76, "size": 7500, "orders": 15},
      {"price": 15.77, "size": 9200, "orders": 22},
      {"price": 15.78, "size": 6800, "orders": 14}
    ]
  },
  "timestamp": "2024-09-24T10:30:15.789Z"
}
```

#### 6. Market Summary
**Server → Client**
```json
{
  "type": "MARKET_SUMMARY",
  "data": {
    "marketStatus": "OPEN",
    "tradingSession": "CONTINUOUS",
    "totalVolume": 125000000,
    "totalValue": 1875000000.00,
    "totalTrades": 25000,
    "advancers": 85,
    "decliners": 45,
    "unchanged": 20,
    "index": {
      "name": "BIST100",
      "value": 8750.25,
      "change": 125.50,
      "changePercent": 1.45
    }
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### 7. Unsubscribe
**Client → Server**
```json
{
  "type": "UNSUBSCRIBE",
  "symbols": ["THYAO"],
  "subscriptionId": "SUB-789123456"
}
```

**Server → Client**
```json
{
  "type": "UNSUBSCRIPTION_CONFIRMED",
  "symbols": ["THYAO"],
  "subscriptionId": "SUB-789123456",
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### 8. Heartbeat
**Server → Client**
```json
{
  "type": "HEARTBEAT",
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

**Client → Server (Response)**
```json
{
  "type": "PONG",
  "timestamp": "2024-09-24T10:30:00.100Z"
}
```

### JavaScript Client Example

```javascript
class MarketDataWebSocket {
    constructor(token) {
        this.ws = null;
        this.token = token;
        this.subscriptions = new Set();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }

    connect() {
        this.ws = new WebSocket(`ws://localhost:8082/ws/market-data?token=${this.token}`);

        this.ws.onopen = (event) => {
            console.log('Market data WebSocket connected');
            this.reconnectAttempts = 0;
            this.authenticate();
        };

        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleMessage(message);
        };

        this.ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code, event.reason);
            this.reconnect();
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
    }

    authenticate() {
        this.send({
            type: 'AUTH',
            token: this.token
        });
    }

    subscribe(symbols, dataTypes = ['QUOTE']) {
        const message = {
            type: 'SUBSCRIBE',
            symbols: symbols,
            dataTypes: dataTypes
        };
        this.send(message);
        symbols.forEach(symbol => this.subscriptions.add(symbol));
    }

    unsubscribe(symbols) {
        const message = {
            type: 'UNSUBSCRIBE',
            symbols: symbols
        };
        this.send(message);
        symbols.forEach(symbol => this.subscriptions.delete(symbol));
    }

    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        }
    }

    handleMessage(message) {
        switch (message.type) {
            case 'AUTH_SUCCESS':
                console.log('Authentication successful');
                break;
            case 'QUOTE':
                this.onQuoteUpdate(message.symbol, message.data);
                break;
            case 'TRADE':
                this.onTradeUpdate(message.symbol, message.data);
                break;
            case 'DEPTH':
                this.onDepthUpdate(message.symbol, message.data);
                break;
            case 'HEARTBEAT':
                this.send({ type: 'PONG', timestamp: new Date().toISOString() });
                break;
        }
    }

    onQuoteUpdate(symbol, data) {
        // Handle quote update
        console.log(`Quote update for ${symbol}:`, data);
    }

    onTradeUpdate(symbol, data) {
        // Handle trade update
        console.log(`Trade update for ${symbol}:`, data);
    }

    onDepthUpdate(symbol, data) {
        // Handle order book depth update
        console.log(`Depth update for ${symbol}:`, data);
    }

    reconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
                console.log(`Reconnecting... Attempt ${this.reconnectAttempts + 1}`);
                this.reconnectAttempts++;
                this.connect();
            }, Math.pow(2, this.reconnectAttempts) * 1000); // Exponential backoff
        }
    }

    disconnect() {
        if (this.ws) {
            this.ws.close(1000, 'Client disconnect');
        }
    }
}

// Usage
const marketData = new MarketDataWebSocket('your_jwt_token');
marketData.connect();

// Subscribe to symbols
setTimeout(() => {
    marketData.subscribe(['AKBNK', 'THYAO', 'GARAN'], ['QUOTE', 'TRADE']);
}, 1000);
```

---

## Trading WebSocket API (Port: 8083)

### Connection Endpoint
`ws://localhost:8083/ws/trading`

### Message Types

#### 1. Order Status Updates
**Server → Client**
```json
{
  "type": "ORDER_UPDATE",
  "data": {
    "orderId": "ORD-123456789",
    "brokerOrderId": "ALG-987654321",
    "symbol": "AKBNK",
    "side": "BUY",
    "orderType": "LIMIT",
    "quantity": 1000,
    "filledQuantity": 500,
    "remainingQuantity": 500,
    "price": 15.75,
    "averagePrice": 15.73,
    "status": "PARTIALLY_FILLED",
    "lastFillPrice": 15.73,
    "lastFillQuantity": 500,
    "lastFillTime": "2024-09-24T10:35:00.000Z",
    "commission": 3.93
  },
  "timestamp": "2024-09-24T10:35:00.123Z"
}
```

#### 2. Trade Execution Notifications
**Server → Client**
```json
{
  "type": "TRADE_EXECUTION",
  "data": {
    "transactionId": "TXN-789123456",
    "orderId": "ORD-123456789",
    "symbol": "AKBNK",
    "side": "BUY",
    "quantity": 500,
    "price": 15.73,
    "amount": 7865.00,
    "commission": 3.93,
    "netAmount": 7868.93,
    "timestamp": "2024-09-24T10:35:00.000Z"
  },
  "timestamp": "2024-09-24T10:35:00.456Z"
}
```

#### 3. Portfolio Updates
**Server → Client**
```json
{
  "type": "PORTFOLIO_UPDATE",
  "data": {
    "symbol": "AKBNK",
    "quantity": 1500,
    "averagePrice": 15.65,
    "currentPrice": 15.75,
    "marketValue": 23625.00,
    "cost": 23475.00,
    "pnl": 150.00,
    "pnlPercent": 0.64,
    "totalPortfolioValue": 125750.50,
    "cashBalance": 25000.00
  },
  "timestamp": "2024-09-24T10:35:00.789Z"
}
```

#### 4. Account Balance Updates
**Server → Client**
```json
{
  "type": "BALANCE_UPDATE",
  "data": {
    "cashBalance": 25000.00,
    "availableBalance": 22000.00,
    "reservedBalance": 3000.00,
    "totalPortfolioValue": 125750.50,
    "buyingPower": 44000.00,
    "marginUsed": 0.00,
    "currency": "TRY"
  },
  "timestamp": "2024-09-24T10:35:00.000Z"
}
```

#### 5. Position Updates
**Server → Client**
```json
{
  "type": "POSITION_UPDATE",
  "data": {
    "positions": [
      {
        "symbol": "AKBNK",
        "quantity": 1500,
        "averagePrice": 15.65,
        "currentPrice": 15.75,
        "marketValue": 23625.00,
        "pnl": 150.00,
        "pnlPercent": 0.64
      }
    ],
    "totalValue": 125750.50,
    "totalPnl": 5750.50,
    "totalPnlPercent": 4.79
  },
  "timestamp": "2024-09-24T10:35:00.000Z"
}
```

### Trading WebSocket Client Example

```javascript
class TradingWebSocket {
    constructor(token) {
        this.ws = null;
        this.token = token;
        this.orderCallbacks = new Map();
    }

    connect() {
        this.ws = new WebSocket(`ws://localhost:8083/ws/trading?token=${this.token}`);

        this.ws.onopen = () => {
            console.log('Trading WebSocket connected');
        };

        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleTradingMessage(message);
        };

        this.ws.onclose = (event) => {
            console.log('Trading WebSocket closed');
        };
    }

    handleTradingMessage(message) {
        switch (message.type) {
            case 'ORDER_UPDATE':
                this.onOrderUpdate(message.data);
                break;
            case 'TRADE_EXECUTION':
                this.onTradeExecution(message.data);
                break;
            case 'PORTFOLIO_UPDATE':
                this.onPortfolioUpdate(message.data);
                break;
            case 'BALANCE_UPDATE':
                this.onBalanceUpdate(message.data);
                break;
        }
    }

    onOrderUpdate(orderData) {
        console.log('Order update:', orderData);

        // Update UI with order status
        const callback = this.orderCallbacks.get(orderData.orderId);
        if (callback) {
            callback(orderData);
        }
    }

    onTradeExecution(tradeData) {
        console.log('Trade executed:', tradeData);

        // Show trade notification
        this.showTradeNotification(tradeData);
    }

    onPortfolioUpdate(portfolioData) {
        console.log('Portfolio updated:', portfolioData);

        // Update portfolio display
        this.updatePortfolioDisplay(portfolioData);
    }

    onBalanceUpdate(balanceData) {
        console.log('Balance updated:', balanceData);

        // Update account balance display
        this.updateBalanceDisplay(balanceData);
    }

    registerOrderCallback(orderId, callback) {
        this.orderCallbacks.set(orderId, callback);
    }

    showTradeNotification(tradeData) {
        // Implementation for trade notification
    }

    updatePortfolioDisplay(portfolioData) {
        // Implementation for portfolio update
    }

    updateBalanceDisplay(balanceData) {
        // Implementation for balance update
    }
}
```

---

## User Notifications WebSocket API (Port: 8081)

### Connection Endpoint
`ws://localhost:8081/ws/notifications`

### Message Types

#### 1. System Notifications
**Server → Client**
```json
{
  "type": "SYSTEM_NOTIFICATION",
  "data": {
    "id": "NOTIF-123456789",
    "title": "Market Closure Notice",
    "message": "BIST will close early today due to national holiday",
    "severity": "INFO",
    "category": "MARKET",
    "expiresAt": "2024-09-24T16:00:00.000Z"
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### 2. Price Alerts
**Server → Client**
```json
{
  "type": "PRICE_ALERT",
  "data": {
    "alertId": "ALERT-987654321",
    "symbol": "AKBNK",
    "alertType": "PRICE_ABOVE",
    "triggerPrice": 16.00,
    "currentPrice": 16.05,
    "message": "AKBNK price has exceeded your alert threshold of 16.00 TRY"
  },
  "timestamp": "2024-09-24T11:15:00.000Z"
}
```

#### 3. Account Notifications
**Server → Client**
```json
{
  "type": "ACCOUNT_NOTIFICATION",
  "data": {
    "notificationId": "ACC-555666777",
    "type": "LOW_BALANCE",
    "title": "Low Account Balance",
    "message": "Your account balance is below 10,000 TRY",
    "currentBalance": 8500.00,
    "threshold": 10000.00,
    "severity": "WARNING"
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

---

## WebSocket Connection Management

### Connection States

| State | Description | Client Action |
|-------|-------------|---------------|
| **CONNECTING** | Initial connection attempt | Wait for open event |
| **OPEN** | Connection established | Send authentication |
| **AUTHENTICATED** | Ready for data exchange | Subscribe to data |
| **CLOSING** | Connection closing | Clean up resources |
| **CLOSED** | Connection closed | Attempt reconnection |

### Reconnection Strategy

```javascript
class ReconnectingWebSocket {
    constructor(url, token) {
        this.url = url;
        this.token = token;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // Initial delay: 1 second
        this.maxReconnectDelay = 30000; // Max delay: 30 seconds
    }

    connect() {
        this.ws = new WebSocket(`${this.url}?token=${this.token}`);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;
        };

        this.ws.onclose = (event) => {
            console.log('WebSocket closed:', event.code);
            if (event.code !== 1000) { // Not a normal closure
                this.scheduleReconnect();
            }
        };
    }

    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            const delay = Math.min(
                this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
                this.maxReconnectDelay
            );

            setTimeout(() => {
                console.log(`Reconnecting... Attempt ${this.reconnectAttempts + 1}`);
                this.reconnectAttempts++;
                this.connect();
            }, delay);
        } else {
            console.error('Maximum reconnection attempts reached');
        }
    }
}
```

### Error Handling

#### Connection Errors
```json
{
  "type": "ERROR",
  "error": {
    "code": "CONNECTION_FAILED",
    "message": "Failed to establish connection",
    "details": "Network timeout"
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

#### Subscription Errors
```json
{
  "type": "SUBSCRIPTION_ERROR",
  "error": {
    "code": "INVALID_SYMBOL",
    "message": "Symbol 'INVALID' not found",
    "symbols": ["INVALID"],
    "validSymbols": ["AKBNK", "THYAO", "GARAN"]
  },
  "timestamp": "2024-09-24T10:30:00.000Z"
}
```

## Performance Considerations

### Message Throttling
- Market data updates are throttled to prevent overwhelming clients
- Maximum 100 messages per second per connection
- Batch updates for multiple symbols when possible

### Connection Limits
- Maximum 10 concurrent connections per user
- Maximum 100 symbol subscriptions per connection
- Automatic cleanup of idle connections (30 minutes)

### Data Compression
- Optional gzip compression for message payloads
- Enabled via WebSocket extension negotiation
- Reduces bandwidth usage by ~60%

## Security

### Authentication
- JWT token required for all connections
- Token validation on connection and periodically during session
- Automatic disconnection on token expiration

### Rate Limiting
- Connection attempts: 5 per minute per IP
- Message rate: 50 messages per second per connection
- Subscription rate: 10 subscriptions per second

### Data Access Control
- Users can only access their own trading data
- Market data access based on subscription level
- Real-time data requires premium subscription

---

**Last Updated:** September 2024
**Version:** 1.0
**Maintainer:** BIST Trading Platform Team
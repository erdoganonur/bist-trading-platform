# AlgoLab WebSocket Integration

## Overview

This package provides WebSocket integration for AlgoLab API, enabling real-time market data streaming.

## Components

### 1. AlgoLabWebSocketClient
Low-level WebSocket client that handles:
- Connection management
- Authentication
- Message parsing
- Heartbeat/ping mechanism
- Auto-reconnect with exponential backoff

### 2. AlgoLabWebSocketService
High-level service that provides:
- Subscription management
- Data stream handlers
- Easy-to-use API for applications

### 3. DTOs
- `TickData` - Real-time price updates
- `OrderBookData` - Level 2 order book
- `TradeData` - Individual trade executions
- `WebSocketMessage` - Generic message wrapper

## Usage Examples

### Basic Connection

```java
@Autowired
private AlgoLabWebSocketService webSocketService;

// Connect using existing session
AlgoLabSession session = sessionManager.loadSession();
webSocketService.connectAsync(session.getToken(), session.getHash())
    .thenAccept(v -> log.info("WebSocket connected"))
    .exceptionally(ex -> {
        log.error("Connection failed", ex);
        return null;
    });
```

### Subscribe to Tick Data

```java
webSocketService.subscribeToTick("AKBNK", tickData -> {
    log.info("Tick: {} - Last: {}, Bid: {}, Ask: {}",
        tickData.getSymbol(),
        tickData.getLastPrice(),
        tickData.getBidPrice(),
        tickData.getAskPrice()
    );
});
```

### Subscribe to Order Book

```java
webSocketService.subscribeToOrderBook("THYAO", orderBook -> {
    log.info("OrderBook: {} - Spread: {}, MidPrice: {}",
        orderBook.getSymbol(),
        orderBook.getSpread(),
        orderBook.getMidPrice()
    );

    // Process bid levels
    orderBook.getBids().forEach(level ->
        log.info("Bid: {} @ {}", level.getQuantity(), level.getPrice())
    );

    // Process ask levels
    orderBook.getAsks().forEach(level ->
        log.info("Ask: {} @ {}", level.getQuantity(), level.getPrice())
    );
});
```

### Subscribe to Trade Data

```java
webSocketService.subscribeToTrade("GARAN", trade -> {
    log.info("Trade: {} - Price: {}, Qty: {}, Side: {}",
        trade.getSymbol(),
        trade.getPrice(),
        trade.getQuantity(),
        trade.getSide()
    );
});
```

### Subscribe to All Data Types

```java
webSocketService.subscribeToAll(
    "AKBNK",
    tickData -> handleTick(tickData),
    orderBook -> handleOrderBook(orderBook),
    trade -> handleTrade(trade)
);
```

### Unsubscribe

```java
// Unsubscribe from specific channel
webSocketService.unsubscribeFromTick("AKBNK");

// Unsubscribe from all channels for a symbol
webSocketService.unsubscribeFromAll("AKBNK");
```

### Check Status

```java
boolean connected = webSocketService.isConnected();
boolean authenticated = webSocketService.isAuthenticated();

Map<String, Object> status = webSocketService.getStatus();
log.info("WebSocket status: {}", status);
```

### Get Active Subscriptions

```java
Set<String> tickSubscriptions = webSocketService.getSubscriptions("tick");
Map<String, Set<String>> allSubscriptions = webSocketService.getAllSubscriptions();
```

## Configuration

Configuration is in `application-dev.yml`:

```yaml
algolab:
  websocket:
    enabled: true
    url: wss://www.algolab.com.tr/api/ws
    auto-connect: true
    heartbeat-interval: 900000  # 15 minutes
    connection-timeout: 30000   # 30 seconds
    reconnect:
      enabled: true
      initial-delay: 1000       # 1 second
      max-delay: 60000          # 60 seconds
      multiplier: 2.0           # exponential backoff
      max-attempts: 0           # 0 = unlimited
```

## Features

### Auto-Reconnect
The client automatically reconnects on connection loss using exponential backoff:
- Initial delay: 1 second
- Delay doubles after each attempt (2s, 4s, 8s, 16s, 32s, 60s max)
- Unlimited attempts by default

### Heartbeat
Automatic ping/pong every 15 minutes to keep connection alive.

### Session Management
WebSocket connection status is tracked in `AlgoLabSession`:
```java
session.isWebsocketConnected()
session.getWebsocketLastConnected()
```

### Thread Safety
All components are thread-safe and can be used concurrently.

## Error Handling

All exceptions are logged and handled gracefully:
- Connection failures trigger auto-reconnect
- Authentication failures close the connection
- Message parsing errors are logged but don't affect other messages

## Performance Considerations

- Subscriptions are tracked in memory
- Message handlers run in WebSocket thread (keep them fast)
- Use separate threads for heavy processing:

```java
webSocketService.subscribeToTick("AKBNK", tickData -> {
    CompletableFuture.runAsync(() -> {
        // Heavy processing here
        processTickData(tickData);
    });
});
```

## Testing

To test WebSocket connection:

```bash
# Set environment variables
export ALGOLAB_API_KEY="your-api-key"
export ALGOLAB_USERNAME="your-username"
export ALGOLAB_PASSWORD="your-password"

# Start application
./gradlew bootRun

# Check logs for:
# "WebSocket connected successfully"
# "WebSocket authentication successful"
```

## Troubleshooting

### Connection Keeps Dropping
- Check network stability
- Verify firewall allows WebSocket connections
- Check AlgoLab server status

### Authentication Fails
- Verify token and hash are valid
- Check if REST API authentication works
- Try manual login flow

### No Data Received
- Check if subscriptions are active: `getSubscriptions()`
- Verify symbol format (e.g., "AKBNK" not "AKBNK.IS")
- Check AlgoLab API documentation for correct channel names

### High Memory Usage
- Unsubscribe from unused channels
- Clear handlers when not needed
- Monitor active subscriptions count

## Integration with Existing Code

The WebSocket service integrates seamlessly with existing AlgoLab REST client:

```java
@Service
public class TradingService {
    @Autowired
    private AlgoLabAuthService authService;

    @Autowired
    private AlgoLabWebSocketService wsService;

    @Autowired
    private AlgoLabOrderService orderService;

    public void start() {
        // Authenticate via REST API
        String token = authService.loginUser();
        String hash = authService.loginUserControl(smsCode);

        // Connect WebSocket
        wsService.connectAsync(token, hash);

        // Subscribe to market data
        wsService.subscribeToTick("AKBNK", this::onTick);

        // Place order via REST API
        orderService.sendOrder("AKBNK", "BUY", ...);
    }

    private void onTick(TickData tick) {
        // React to real-time data
    }
}
```

## Next Steps

1. Implement market data caching
2. Add TimescaleDB integration for historical data
3. Implement backpressure handling
4. Add metrics and monitoring
5. Create WebSocket health check endpoint

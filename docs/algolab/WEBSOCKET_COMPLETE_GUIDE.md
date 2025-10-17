# AlgoLab WebSocket Integration - Complete Guide

**Status:** ✅ WORKING - Messages are being received successfully

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Authentication](#authentication)
4. [Message Format](#message-format)
5. [Implementation](#implementation)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)
8. [CLI Integration](#cli-integration)

---

## Overview

The AlgoLab WebSocket integration provides real-time market data streaming from Borsa Istanbul (BIST). This implementation successfully connects, authenticates, and receives tick data for forex and stock symbols.

### Key Features

- ✅ Real-time tick data streaming
- ✅ Automatic reconnection with exponential backoff
- ✅ Session-based authentication
- ✅ HTTP polling API for CLI clients
- ✅ Message buffering for HTTP access
- ✅ Heartbeat/keep-alive mechanism

### Current Status

**Working Components:**
- WebSocket connection (/api/ws endpoint)
- Authentication with APIKEY, Authorization, and Checker headers
- Message reception (Tick data for USDTRY, FOREX symbols)
- Automatic subscription on connection
- Message buffering for HTTP polling
- CLI real-time display

**Tested Symbols:**
- ✅ USDTRY (FOREX - 24/7 available)
- ✅ EURTRY (FOREX - 24/7 available)
- ⏳ AKBNK, THYAO (Stocks - only during market hours 10:00-18:00 GMT+3)

---

## Architecture

### Component Flow

```
AlgoLab Server → WebSocketClient → MessageBuffer → HTTP API → CLI
     (WSS)          (Spring)         (In-Memory)     (REST)    (Python)
```

### Key Classes

1. **AlgoLabWebSocketClient** - Core WebSocket client
   - Location: `src/main/java/com/bisttrading/broker/algolab/websocket/AlgoLabWebSocketClient.java`
   - Handles connection, authentication, message parsing
   - Implements reconnection logic

2. **WebSocketMessageBuffer** - Message buffering service
   - Location: `src/main/java/com/bisttrading/broker/algolab/service/WebSocketMessageBuffer.java`
   - Buffers last 100 messages per symbol
   - Provides HTTP access to WebSocket data

3. **BrokerController** - HTTP API endpoints
   - Location: `src/main/java/com/bisttrading/broker/controller/BrokerController.java`
   - Exposes buffered messages via REST

---

## Authentication

### WebSocket Headers

AlgoLab WebSocket requires three special headers:

```java
headers.add("APIKEY", apiKey);  // AlgoLab API key
headers.add("Authorization", hash);  // JWT token from login
headers.add("Checker", checker);  // SHA-256 hash for verification
```

### Checker Calculation

```java
String checkerData = apiKey + hostname + "/ws";
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hashBytes = digest.digest(checkerData.getBytes(StandardCharsets.UTF_8));
String checker = bytesToHex(hashBytes);  // Hex-encoded SHA-256
```

### Configuration

```yaml
# application-dev.yml
algolab:
  api:
    key: ${ALGOLAB_API_KEY:API-I1G7BdhIZ3RY/lxNXgqOlFT0bAILG7zmdqwtiagSnDM=}
    hostname: https://www.algolab.com.tr
  websocket:
    enabled: true
    url: wss://www.algolab.com.tr/api/ws  # CRITICAL: Use /api/ws
    auto-connect: true
    heartbeat-interval: 900000  # 15 minutes
```

---

## Message Format

### AlgoLab Format

```json
{
  "Type": "T",  // T=Tick, D=Depth, O=Order
  "Content": {
    "Symbol": "USDTRY",
    "Price": 34.1234,
    "lastVolume": 1000,
    "bid": 34.12,
    "ask": 34.13,
    "timestamp": "2025-10-17T12:00:00Z"
  }
}
```

### Subscription Message

**CRITICAL:** AlgoLab closes the connection if no subscription is sent immediately after connection.

```json
{
  "token": "eyJhbGci...",  // JWT hash from authentication
  "Type": "T",             // T=Tick, D=Depth, O=Order
  "Symbols": ["USDTRY"]    // Array of symbols to subscribe
}
```

This is sent automatically in `afterConnectionEstablished()`:

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    // Send immediate subscription to prevent connection close
    Map<String, Object> subscribeMsg = Map.of(
        "token", authHash,
        "Type", "T",
        "Symbols", new String[]{"USDTRY"}
    );
    String json = objectMapper.writeValueAsString(subscribeMsg);
    session.sendMessage(new TextMessage(json));
}
```

---

## Implementation

### 1. WebSocket Client Setup

```java
@Component
public class AlgoLabWebSocketClient extends TextWebSocketHandler {

    private final WebSocketMessageBuffer messageBuffer;

    public CompletableFuture<Void> connect(String token, String hash) {
        // Calculate headers
        String apiKey = properties.getApi().getKey();
        String checker = calculateChecker(apiKey);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("APIKEY", apiKey);
        headers.add("Authorization", hash);
        headers.add("Checker", checker);

        // Connect
        return webSocketClient.doHandshake(this, headers, uri);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Map<String, Object> rawMessage = objectMapper.readValue(
            message.getPayload(),
            new TypeReference<Map<String, Object>>() {}
        );

        if (rawMessage.containsKey("Type") && rawMessage.containsKey("Content")) {
            handleAlgoLabMessage(rawMessage);
        }
    }

    private void handleAlgoLabMessage(Map<String, Object> message) {
        String type = (String) message.get("Type");
        Object content = message.get("Content");

        switch (type) {
            case "T": // Tick data
                TickData tickData = objectMapper.convertValue(content, TickData.class);
                messageBuffer.addTick(tickData.getSymbol(), tickData);
                notifyHandler(WebSocketMessage.Type.TICK, tickData);
                break;
        }
    }
}
```

### 2. HTTP API Endpoints

```java
@GetMapping("/websocket/stream/ticks/{symbol}")
public ResponseEntity<Map<String, Object>> getRecentTicks(
        @PathVariable String symbol,
        @RequestParam(defaultValue = "10") int limit) {

    var messages = messageBuffer.getRecentTicks(symbol, limit);

    return ResponseEntity.ok(Map.of(
        "symbol", symbol,
        "count", messages.size(),
        "messages", messages
    ));
}
```

### 3. CLI Polling Implementation

```python
def view_realtime_ticks(self):
    """Display real-time tick data via HTTP polling."""
    from rich.live import Live
    import time

    with Live(create_table([]), refresh_per_second=2) as live:
        while True:
            # Poll backend every second
            response = self.api.get(f"/api/v1/broker/websocket/stream/ticks/{symbol}?limit=15")
            messages = response.get("messages", [])

            if messages:
                live.update(create_table(messages))

            time.sleep(1)
```

---

## Testing

### 1. Backend WebSocket Test

```bash
curl -X POST http://localhost:8081/api/test/websocket/connect-v2
```

Expected response:
```json
{
  "success": true,
  "message": "WebSocket V2 connected and subscription sent!",
  "connected": true
}
```

### 2. Check Backend Logs

```bash
tail -f logs/application.log | grep "✅ Received TICK"
```

Expected output:
```
✅ Received TICK data: Symbol=USDTRY, Price=34.1234
✅ Received TICK data: Symbol=USDTRY, Price=34.1245
```

### 3. Test HTTP Stream API

```bash
# Get recent ticks
curl http://localhost:8081/api/v1/broker/websocket/stream/ticks/USDTRY?limit=5

# Get stream statistics
curl http://localhost:8081/api/v1/broker/websocket/stream/stats

# Get active symbols
curl http://localhost:8081/api/v1/broker/websocket/stream/symbols
```

### 4. Test CLI

```bash
cd cli-client
./start.sh

# Menu: 2 → 5 (Real-Time Tick Data)
# Enter symbol: USDTRY
```

---

## Troubleshooting

### Connection Closes Immediately (Code 1006)

**Symptom:** WebSocket connects but closes within milliseconds

**Cause:** No subscription message sent after connection

**Solution:** Ensure `afterConnectionEstablished()` sends subscription immediately

### No Messages Received

**Symptoms:**
- Connection successful
- No tick data in logs
- Empty buffer

**Possible Causes:**

1. **Market Closed** (for stocks like AKBNK)
   - BIST hours: 10:00-18:00 GMT+3 (Monday-Friday)
   - Solution: Use FOREX symbols (USDTRY, EURTRY) which are 24/7

2. **Wrong Symbol**
   - Check symbol is valid for AlgoLab
   - Solution: Test with USDTRY first

3. **Subscription Not Sent**
   - Check logs for "✅ IMMEDIATE subscription sent"
   - Solution: Verify `afterConnectionEstablished()` logic

### Authentication Failed

**Symptom:** Connection rejected or 401 error

**Possible Causes:**

1. **Invalid Session**
   - Session expired or not saved
   - Solution: Perform fresh login via CLI or REST API

2. **Wrong Headers**
   - Missing APIKEY, Authorization, or Checker
   - Solution: Verify header calculation in `connect()` method

3. **Invalid Checker**
   - Wrong SHA-256 calculation
   - Solution: Ensure checker = SHA256(APIKEY + hostname + "/ws")

---

## CLI Integration

### Architecture

```
Backend WebSocket → Buffer → HTTP API → CLI Polling → Rich Display
```

### Features

1. **Real-Time Tick Stream**
   - Updates every second via HTTP polling
   - Shows last 15 tick messages
   - Live updating table with Rich library

2. **Real-Time Trade Stream**
   - Updates every second via HTTP polling
   - Shows executed trades
   - Color-coded buy/sell indicators

3. **Automatic Fallback**
   - Shows "waiting for messages" if buffer is empty
   - Graceful error handling
   - Reconnection support

### Usage

```bash
cd cli-client
./start.sh

# Login with platform credentials
# Navigate: 2 (Broker) → 5 (Real-Time Ticks)
# Enter symbol: USDTRY
# Press Ctrl+C to exit
```

---

## Performance Considerations

### Buffer Size

- Maximum 100 messages per symbol
- Messages older than 1 minute are auto-cleaned
- Prevents memory growth

### HTTP Polling Rate

- CLI polls every 1 second
- Minimal backend overhead
- Configurable via `time.sleep(1)`

### WebSocket Reconnection

- Exponential backoff (1s → 2s → 4s → ... → 60s max)
- Maximum 3 attempts in dev, unlimited in production
- Automatic on connection loss

---

## Quick Reference

### Working URL
```
wss://www.algolab.com.tr/api/ws
```

### Required Headers
```
APIKEY: <algolab-api-key>
Authorization: <jwt-hash>
Checker: <sha256-hex>
```

### Working Symbols (24/7)
```
USDTRY, EURTRY, GBPTRY, EURGBP, EURUSD
```

### Stock Symbols (Market Hours Only)
```
AKBNK, THYAO, GARAN, ISCTR, TUPRS
```

### Test Endpoints
```
POST /api/test/websocket/connect-v2
GET  /api/v1/broker/websocket/stream/ticks/{symbol}
GET  /api/v1/broker/websocket/stream/stats
```

---

## Related Documentation

- [AlgoLab API Endpoints](./ALGOLAB_API_ENDPOINTS.md)
- [Authentication Flow](./ALGOLAB_AUTHENTICATION_FLOW.md)
- [Python to Java Mapping](./PYTHON_TO_JAVA_MAPPING.md)
- [WebSocket API](../api/websocket-api.md)

---

**Last Updated:** 2025-10-17
**Status:** ✅ Production Ready

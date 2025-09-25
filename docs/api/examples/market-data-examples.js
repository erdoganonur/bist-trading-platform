/**
 * BIST Trading Platform - Market Data WebSocket API Examples
 * Market Data Service - Port 8082
 *
 * This file contains JavaScript examples for connecting to and using the
 * Market Data WebSocket API for real-time market data streaming.
 *
 * Prerequisites:
 * 1. Obtain JWT token from User Management Service (auth-examples.http)
 * 2. Ensure Market Data Service is running on port 8082
 * 3. Have a modern browser or Node.js environment with WebSocket support
 */

// Configuration
const MARKET_DATA_WS_URL = 'ws://localhost:8082/ws/market-data';
const MARKET_DATA_REST_URL = 'http://localhost:8082/api/v1/market-data';

// You need to get this token from authentication service first
// See auth-examples.http for authentication examples
const JWT_TOKEN = 'YOUR_JWT_TOKEN_HERE';

/**
 * Example 1: Basic WebSocket Connection and Authentication
 */
class MarketDataWebSocket {
    constructor(token) {
        this.ws = null;
        this.token = token;
        this.subscriptions = new Set();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 1000; // Initial delay: 1 second
        this.heartbeatInterval = null;
    }

    connect() {
        console.log('🔌 Connecting to Market Data WebSocket...');

        // Connect with token as query parameter
        this.ws = new WebSocket(`${MARKET_DATA_WS_URL}?token=${this.token}`);

        this.ws.onopen = (event) => {
            console.log('✅ WebSocket connected successfully');
            this.reconnectAttempts = 0;
            this.reconnectDelay = 1000;

            // Start authentication
            this.authenticate();

            // Start heartbeat
            this.startHeartbeat();
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                this.handleMessage(message);
            } catch (error) {
                console.error('❌ Error parsing message:', error, event.data);
            }
        };

        this.ws.onclose = (event) => {
            console.log(`🔌 WebSocket closed: ${event.code} - ${event.reason}`);
            this.stopHeartbeat();

            if (event.code !== 1000) { // Not a normal closure
                this.scheduleReconnect();
            }
        };

        this.ws.onerror = (error) => {
            console.error('❌ WebSocket error:', error);
        };
    }

    authenticate() {
        const authMessage = {
            type: 'AUTH',
            token: this.token
        };

        console.log('🔐 Sending authentication...');
        this.send(authMessage);
    }

    handleMessage(message) {
        switch (message.type) {
            case 'AUTH_SUCCESS':
                console.log('✅ Authentication successful');
                console.log('User ID:', message.userId);
                console.log('Subscription Level:', message.subscriptionLevel);
                break;

            case 'AUTH_ERROR':
                console.error('❌ Authentication failed:', message.error);
                break;

            case 'SUBSCRIPTION_CONFIRMED':
                console.log('✅ Subscription confirmed:');
                console.log('  Symbols:', message.symbols);
                console.log('  Data Types:', message.dataTypes);
                console.log('  Subscription ID:', message.subscriptionId);
                break;

            case 'QUOTE':
                this.handleQuoteUpdate(message.symbol, message.data, message.timestamp);
                break;

            case 'TRADE':
                this.handleTradeUpdate(message.symbol, message.data, message.timestamp);
                break;

            case 'DEPTH':
                this.handleDepthUpdate(message.symbol, message.data, message.timestamp);
                break;

            case 'MARKET_SUMMARY':
                this.handleMarketSummary(message.data, message.timestamp);
                break;

            case 'HEARTBEAT':
                this.handleHeartbeat(message.timestamp);
                break;

            case 'ERROR':
                console.error('❌ Server error:', message.error);
                break;

            default:
                console.log('📨 Unknown message type:', message.type, message);
        }
    }

    handleQuoteUpdate(symbol, data, timestamp) {
        console.log(`📊 ${symbol} Quote Update:`, {
            lastPrice: data.lastPrice,
            bidPrice: data.bidPrice,
            askPrice: data.askPrice,
            change: data.change,
            changePercent: data.changePercent + '%',
            volume: data.volume,
            timestamp: new Date(timestamp).toLocaleTimeString('tr-TR')
        });

        // Update UI elements
        this.updateQuoteDisplay(symbol, data);
    }

    handleTradeUpdate(symbol, data, timestamp) {
        console.log(`💼 ${symbol} Trade:`, {
            tradeId: data.tradeId,
            price: data.price,
            quantity: data.quantity,
            side: data.side,
            timestamp: new Date(timestamp).toLocaleTimeString('tr-TR')
        });
    }

    handleDepthUpdate(symbol, data, timestamp) {
        console.log(`📈 ${symbol} Order Book Update:`, {
            bestBid: data.bids[0] ? `${data.bids[0].price} (${data.bids[0].size})` : 'N/A',
            bestAsk: data.asks[0] ? `${data.asks[0].price} (${data.asks[0].size})` : 'N/A',
            spread: data.bids[0] && data.asks[0] ?
                    (data.asks[0].price - data.bids[0].price).toFixed(4) : 'N/A',
            timestamp: new Date(timestamp).toLocaleTimeString('tr-TR')
        });
    }

    handleMarketSummary(data, timestamp) {
        console.log('🏛️ Market Summary:', {
            marketStatus: data.marketStatus,
            totalVolume: data.totalVolume.toLocaleString('tr-TR'),
            totalValue: data.totalValue.toLocaleString('tr-TR') + ' TRY',
            advancers: data.advancers,
            decliners: data.decliners,
            unchanged: data.unchanged,
            bistIndex: data.index ? `${data.index.value} (${data.index.changePercent}%)` : 'N/A',
            timestamp: new Date(timestamp).toLocaleTimeString('tr-TR')
        });
    }

    handleHeartbeat(timestamp) {
        // Respond to heartbeat
        this.send({
            type: 'PONG',
            timestamp: new Date().toISOString()
        });
    }

    // Subscription methods
    subscribe(symbols, dataTypes = ['QUOTE']) {
        const subscribeMessage = {
            type: 'SUBSCRIBE',
            symbols: Array.isArray(symbols) ? symbols : [symbols],
            dataTypes: dataTypes
        };

        console.log('📡 Subscribing to:', subscribeMessage);
        this.send(subscribeMessage);

        // Keep track of subscriptions
        symbols.forEach(symbol => this.subscriptions.add(symbol));
    }

    unsubscribe(symbols) {
        const unsubscribeMessage = {
            type: 'UNSUBSCRIBE',
            symbols: Array.isArray(symbols) ? symbols : [symbols]
        };

        console.log('🚫 Unsubscribing from:', unsubscribeMessage);
        this.send(unsubscribeMessage);

        // Remove from subscriptions
        symbols.forEach(symbol => this.subscriptions.delete(symbol));
    }

    // Utility methods
    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            console.warn('⚠️  WebSocket not ready, message not sent:', message);
        }
    }

    startHeartbeat() {
        // Send ping every 30 seconds to keep connection alive
        this.heartbeatInterval = setInterval(() => {
            this.send({
                type: 'PING',
                timestamp: new Date().toISOString()
            });
        }, 30000);
    }

    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts);

            console.log(`🔄 Reconnecting in ${delay}ms... (attempt ${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);

            setTimeout(() => {
                this.reconnectAttempts++;
                this.connect();
            }, delay);
        } else {
            console.error('❌ Maximum reconnection attempts reached');
        }
    }

    updateQuoteDisplay(symbol, data) {
        // Example UI update - you can customize this for your application
        const displayElement = document.getElementById(`quote-${symbol}`);
        if (displayElement) {
            displayElement.innerHTML = `
                <div class="quote-card">
                    <h3>${symbol}</h3>
                    <div class="price ${data.change >= 0 ? 'positive' : 'negative'}">
                        ${data.lastPrice} TRY
                        <span class="change">(${data.change >= 0 ? '+' : ''}${data.change} / ${data.changePercent}%)</span>
                    </div>
                    <div class="bid-ask">
                        <span>Bid: ${data.bidPrice}</span>
                        <span>Ask: ${data.askPrice}</span>
                    </div>
                    <div class="volume">Volume: ${data.volume.toLocaleString('tr-TR')}</div>
                </div>
            `;
        }
    }

    disconnect() {
        console.log('🔌 Disconnecting WebSocket...');

        this.stopHeartbeat();

        if (this.ws) {
            this.ws.close(1000, 'Client disconnect');
        }
    }
}

/**
 * Example 2: REST API Integration for Historical Data
 */
class MarketDataRestAPI {
    constructor(token) {
        this.token = token;
        this.baseUrl = MARKET_DATA_REST_URL;
    }

    async makeRequest(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const config = {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        };

        try {
            console.log(`🌐 API Request: ${config.method} ${url}`);
            const response = await fetch(url, config);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('❌ API Error:', error);
            throw error;
        }
    }

    // Get multi-timeframe OHLCV data
    async getOHLCVData(symbol, startTime, endTime) {
        const endpoint = `/ohlcv/${symbol}?startTime=${startTime}&endTime=${endTime}`;
        const data = await this.makeRequest(endpoint);

        console.log(`📊 OHLCV Data for ${symbol}:`, {
            timeframes: Object.keys(data),
            totalDataPoints: Object.values(data).reduce((sum, arr) => sum + arr.length, 0)
        });

        return data;
    }

    // Get volume analysis
    async getVolumeAnalysis(symbol, startTime, endTime) {
        const endpoint = `/volume/${symbol}?startTime=${startTime}&endTime=${endTime}`;
        const data = await this.makeRequest(endpoint);

        console.log(`📈 Volume Analysis for ${symbol}:`, {
            totalVolume: data.totalVolume?.toLocaleString('tr-TR'),
            vwap: data.vwap,
            analysisTime: new Date(data.analysisTime).toLocaleString('tr-TR')
        });

        return data;
    }

    // Get technical indicators
    async getTechnicalIndicators(symbol, timeframe, startTime, endTime) {
        const endpoint = `/technical/${symbol}?timeframe=${encodeURIComponent(timeframe)}&startTime=${startTime}&endTime=${endTime}`;
        const data = await this.makeRequest(endpoint);

        console.log(`📊 Technical Indicators for ${symbol} (${timeframe}):`, {
            sma20Points: data.sma20?.length,
            sma50Points: data.sma50?.length,
            rsiPoints: data.rsi?.length,
            calculatedAt: new Date(data.calculatedAt).toLocaleString('tr-TR')
        });

        return data;
    }

    // Get order book analysis
    async getOrderBookAnalysis(symbol, startTime, endTime) {
        const endpoint = `/orderbook/${symbol}?startTime=${startTime}&endTime=${endTime}`;
        const data = await this.makeRequest(endpoint);

        console.log(`📋 Order Book Analysis for ${symbol}:`, {
            spreadAnalysisPoints: data.spreadAnalysis?.length,
            depthAnalysisPoints: data.depthAnalysis?.length,
            analysisTime: new Date(data.analysisTime).toLocaleString('tr-TR')
        });

        return data;
    }

    // Get market overview (admin only)
    async getMarketOverview(startTime, endTime) {
        const endpoint = `/overview?startTime=${startTime}&endTime=${endTime}`;
        const data = await this.makeRequest(endpoint);

        console.log('🏛️ Market Overview:', {
            activeSymbolCount: data.activeSymbolCount,
            topVolumeSymbols: data.topVolumeSymbols?.length,
            analysisTime: new Date(data.analysisTime).toLocaleString('tr-TR')
        });

        return data;
    }
}

/**
 * Example 3: Complete Usage Example
 */
async function runMarketDataExamples() {
    console.log('🚀 Starting BIST Trading Platform Market Data Examples');

    // Check if we have a valid token
    if (JWT_TOKEN === 'YOUR_JWT_TOKEN_HERE') {
        console.error('❌ Please set a valid JWT_TOKEN first!');
        console.log('💡 Use auth-examples.http to get a token from User Management Service');
        return;
    }

    // Example 1: WebSocket Real-time Data
    console.log('\n📡 === WebSocket Real-time Data Example ===');

    const marketDataWS = new MarketDataWebSocket(JWT_TOKEN);
    marketDataWS.connect();

    // Wait for connection and authentication
    await new Promise(resolve => setTimeout(resolve, 2000));

    // Subscribe to major BIST stocks
    marketDataWS.subscribe(['AKBNK', 'THYAO', 'GARAN', 'ISCTR'], ['QUOTE', 'TRADE', 'DEPTH']);

    // Example 2: REST API Historical Data
    console.log('\n📊 === REST API Historical Data Example ===');

    const marketDataAPI = new MarketDataRestAPI(JWT_TOKEN);
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

    try {
        // Get OHLCV data for AKBNK
        const ohlcvData = await marketDataAPI.getOHLCVData(
            'AKBNK',
            oneHourAgo.toISOString(),
            now.toISOString()
        );

        // Get volume analysis
        const volumeAnalysis = await marketDataAPI.getVolumeAnalysis(
            'THYAO',
            oneHourAgo.toISOString(),
            now.toISOString()
        );

        // Get technical indicators
        const technicalData = await marketDataAPI.getTechnicalIndicators(
            'GARAN',
            '5 minutes',
            oneHourAgo.toISOString(),
            now.toISOString()
        );

    } catch (error) {
        console.error('❌ REST API example failed:', error);
    }

    // Let the WebSocket run for a while to show real-time data
    console.log('\n⏱️  Running for 30 seconds to show real-time data...');
    setTimeout(() => {
        console.log('\n🛑 Stopping examples...');
        marketDataWS.disconnect();
    }, 30000);
}

/**
 * Example 4: Portfolio Integration
 * Shows how to combine market data with portfolio information
 */
class PortfolioMarketDataIntegration {
    constructor(marketDataToken, portfolioPositions) {
        this.marketDataWS = new MarketDataWebSocket(marketDataToken);
        this.positions = new Map(portfolioPositions.map(p => [p.symbol, p]));
        this.currentPrices = new Map();

        // Override quote handler to calculate P&L
        this.marketDataWS.handleQuoteUpdate = (symbol, data, timestamp) => {
            this.updatePortfolioValue(symbol, data);
        };
    }

    updatePortfolioValue(symbol, quoteData) {
        const position = this.positions.get(symbol);
        if (!position) return;

        const currentPrice = quoteData.lastPrice;
        const pnl = (currentPrice - position.averagePrice) * position.quantity;
        const pnlPercent = ((currentPrice - position.averagePrice) / position.averagePrice) * 100;

        console.log(`💰 ${symbol} P&L Update:`, {
            position: position.quantity,
            averagePrice: position.averagePrice,
            currentPrice: currentPrice,
            unrealizedPnl: pnl.toFixed(2) + ' TRY',
            pnlPercent: pnlPercent.toFixed(2) + '%',
            change: quoteData.changePercent + '%'
        });
    }

    startMonitoring() {
        this.marketDataWS.connect();

        // Subscribe to all symbols in portfolio
        setTimeout(() => {
            const symbols = Array.from(this.positions.keys());
            this.marketDataWS.subscribe(symbols, ['QUOTE']);
            console.log('📈 Monitoring portfolio symbols:', symbols);
        }, 2000);
    }
}

/**
 * Example 5: Market Scanner
 * Scans market for specific conditions
 */
class MarketScanner {
    constructor(token, conditions) {
        this.marketDataWS = new MarketDataWebSocket(token);
        this.conditions = conditions;
        this.alerts = [];

        // Override quote handler for scanning
        this.marketDataWS.handleQuoteUpdate = (symbol, data, timestamp) => {
            this.scanConditions(symbol, data);
        };
    }

    scanConditions(symbol, data) {
        this.conditions.forEach(condition => {
            if (condition.check(symbol, data)) {
                const alert = {
                    symbol,
                    condition: condition.name,
                    data,
                    timestamp: new Date().toISOString()
                };

                this.alerts.push(alert);
                console.log('🚨 ALERT:', alert);

                // You could send notifications, emails, etc. here
            }
        });
    }
}

// Example scanner conditions
const scannerConditions = [
    {
        name: 'High Volume',
        check: (symbol, data) => data.volume > 1000000
    },
    {
        name: 'Big Mover Up',
        check: (symbol, data) => data.changePercent > 5
    },
    {
        name: 'Big Mover Down',
        check: (symbol, data) => data.changePercent < -5
    },
    {
        name: 'Wide Spread',
        check: (symbol, data) => {
            const spread = ((data.askPrice - data.bidPrice) / data.bidPrice) * 100;
            return spread > 1; // More than 1% spread
        }
    }
];

// HTML Example for browser integration
const htmlExample = `
<!DOCTYPE html>
<html>
<head>
    <title>BIST Market Data</title>
    <style>
        .quote-card { border: 1px solid #ddd; margin: 10px; padding: 15px; border-radius: 5px; }
        .positive { color: green; }
        .negative { color: red; }
        .change { font-size: 0.9em; }
        .bid-ask { margin: 5px 0; font-size: 0.9em; color: #666; }
        .volume { font-size: 0.8em; color: #888; }
    </style>
</head>
<body>
    <h1>BIST Real-time Quotes</h1>
    <div id="quotes">
        <div id="quote-AKBNK"></div>
        <div id="quote-THYAO"></div>
        <div id="quote-GARAN"></div>
        <div id="quote-ISCTR"></div>
    </div>

    <script src="market-data-examples.js"></script>
    <script>
        // Initialize with your JWT token
        const marketData = new MarketDataWebSocket('YOUR_JWT_TOKEN');
        marketData.connect();

        setTimeout(() => {
            marketData.subscribe(['AKBNK', 'THYAO', 'GARAN', 'ISCTR'], ['QUOTE']);
        }, 2000);
    </script>
</body>
</html>
`;

// Export for Node.js usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        MarketDataWebSocket,
        MarketDataRestAPI,
        PortfolioMarketDataIntegration,
        MarketScanner,
        scannerConditions,
        runMarketDataExamples
    };
}

// Auto-run examples if in browser and token is set
if (typeof window !== 'undefined' && JWT_TOKEN !== 'YOUR_JWT_TOKEN_HERE') {
    // runMarketDataExamples();
    console.log('💡 Market Data examples loaded. Call runMarketDataExamples() to start.');
}
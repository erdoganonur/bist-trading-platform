# BIST Trading Platform - Frontend Setup Guide

## ✅ What's Implemented

### Core Architecture
- ✅ Vite + React 18.3 + TypeScript 5.6
- ✅ WebSocket-first architecture for real-time data
- ✅ Zustand for global state management
- ✅ TanStack Query for server state
- ✅ Ant Design 5.x UI components
- ✅ Axios with JWT token refresh interceptor

### Services
- ✅ **WebSocket Service** - Full implementation with:
  - Authentication flow
  - Auto-reconnection with exponential backoff
  - Channel subscriptions (TICK, ORDER_BOOK, TRADE)
  - Heartbeat mechanism
  - Type-safe message handling

- ✅ **REST API Services**:
  - Auth service (login, register, logout, refresh)
  - Market service (symbols, positions)
  - Trading service (orders, portfolio)
  - Axios interceptors with automatic token refresh

### State Management
- ✅ **Auth Store** - User authentication, JWT tokens
- ✅ **Market Data Store** - Real-time ticks, order books, trades, watchlist
- ✅ **WebSocket Store** - Connection status tracking

### Custom Hooks
- ✅ **useWebSocket** - WebSocket connection management
- ✅ **useAuth** - Authentication flow
- ✅ **useMarketData** - Real-time market data with auto-subscribe/unsubscribe

### Pages & Features
- ✅ **Login Page** - Full authentication UI with Ant Design
- ✅ **Dashboard Page** - Real-time market data display
- ✅ **Protected Routes** - Route guards for authentication
- ✅ **Public Routes** - Redirect to dashboard if logged in

### Configuration
- ✅ Vite config with path aliases
- ✅ TypeScript config with strict mode
- ✅ Environment variables setup
- ✅ Code splitting configuration
- ✅ Production build optimization

## 🚀 Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Backend
From the main repository:
```bash
cd /Users/onurerdogan/dev/bist-trading-platform
./start-monolith.sh
```

### 3. Start Frontend
```bash
npm run dev
```

Frontend will be available at: http://localhost:3000

### 4. Test the Application

1. **Login** - Navigate to http://localhost:3000/login
   - Use credentials from backend (create a user via API if needed)

2. **WebSocket Connection** - After login, check browser console:
   ```
   [WebSocket] Connecting to: ws://localhost:8080/api/v1/broker/websocket
   [WebSocket] Connected
   [WebSocket] Authenticated
   ```

3. **Real-time Data** - Add symbols to watchlist and see live updates

## 📁 Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── App.tsx           ✅ Main application component
│   │   ├── router.tsx        ✅ React Router configuration
│   │   └── store.ts          ✅ Zustand stores (auth, market, websocket)
│   ├── services/
│   │   ├── websocket/        ✅ WebSocket service & types
│   │   └── api/              ✅ REST API services (auth, market, trading)
│   ├── hooks/                ✅ Custom hooks (useWebSocket, useAuth, useMarketData)
│   ├── features/
│   │   ├── auth/pages/       ✅ LoginPage
│   │   └── dashboard/pages/  ✅ DashboardPage
│   ├── components/           📂 Ready for components (atoms, molecules, organisms)
│   ├── types/                📂 Ready for shared types
│   └── utils/                📂 Ready for utilities
├── .env.development          ✅ Development environment config
├── vite.config.ts            ✅ Vite configuration with path aliases
├── tsconfig.app.json         ✅ TypeScript config with strict mode
└── package.json              ✅ Dependencies configured
```

## 🔌 WebSocket Architecture

### Connection Flow
1. User logs in → Get JWT access token
2. `useAuth` hook stores token in localStorage and Zustand
3. `useWebSocket` hook auto-connects using token
4. WebSocket sends AUTH message
5. Backend authenticates and responds with AUTH_SUCCESS
6. Connection ready for subscriptions

### Message Types
- `AUTH` - Authenticate with JWT token
- `SUBSCRIBE/UNSUBSCRIBE` - Subscribe to market data channels
- `TICK` - Real-time price updates
- `ORDER_BOOK` - Order book snapshots
- `TRADE` - Trade executions
- `PING/PONG` - Heartbeat every 30 seconds

### Usage Example
```typescript
import { useMarketData } from '@hooks/useMarketData';

function StockCard({ symbol }: { symbol: string }) {
  const { tick, orderBook, trades } = useMarketData(symbol);

  return (
    <div>
      <h2>{symbol}</h2>
      <p>Price: ₺{tick?.lastPrice}</p>
      <p>Change: {tick?.changePercent}%</p>
    </div>
  );
}
```

## 🛠️ Development Commands

```bash
npm run dev          # Start dev server (port 3000)
npm run build        # Build for production
npm run preview      # Preview production build
```

## 🧪 Testing WebSocket Connection

### Browser Console

After login, you should see:
```
[WebSocket] Connecting to: ws://localhost:8080/api/v1/broker/websocket
[WebSocket] Connected
[WebSocket] Authenticated
[useWebSocket] Connection established
```

### Network Tab

1. Open DevTools → Network → WS
2. Find WebSocket connection
3. Watch messages flowing:
   - Outgoing: `{"type":"AUTH","data":{"token":"..."}}`
   - Incoming: `{"type":"AUTH_SUCCESS"}`
   - Outgoing: `{"type":"SUBSCRIBE","channel":"TICK","data":{"symbol":"AKBNK"}}`
   - Incoming: `{"type":"TICK","data":{...price data...}}`

## 🎨 Next Steps (Not Yet Implemented)

### Components to Build
- **Atoms**: Button, Input, Badge, Icon, Spinner, etc.
- **Molecules**: StockCard, OrderForm, PriceDisplay, StatCard
- **Organisms**: Watchlist, TradingChart, OrdersTable, PositionsTable, Header

### Features to Implement
1. **Trading Page** - Full trading interface with:
   - Real-time chart (TradingView Lightweight Charts)
   - Order entry form
   - Order book visualization
   - Trade history

2. **Portfolio Page** - Portfolio management with:
   - Positions list
   - P&L calculations
   - Sector allocation chart
   - Performance metrics

3. **Symbol Search** - Quick symbol search with autocomplete

4. **Order Management** - View and manage orders

5. **Real-time Charts** - Integrate TradingView charts

## 📊 State Flow

```
Login → JWT Token → WebSocket Connect → Subscribe to Symbols
                                            ↓
Backend Sends TICK Messages → WebSocket Service → Zustand Store → React Components → UI Updates
```

## 🚨 Important Notes

1. **Backend Must Be Running** - Frontend requires backend on port 8080
2. **CORS** - Backend must allow http://localhost:3000
3. **WebSocket Endpoint** - Backend must have WebSocket endpoint at `/api/v1/broker/websocket`
4. **Authentication** - JWT token must be valid and not expired
5. **Environment Variables** - Check `.env.development` for correct URLs

## 📝 Environment Variables

### Development (.env.development)
```env
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_BASE_URL=ws://localhost:8080
VITE_API_TIMEOUT=30000
VITE_WS_RECONNECT_DELAY=3000
VITE_WS_MAX_RECONNECT_ATTEMPTS=5
```

### Production (.env.production)
```env
VITE_API_BASE_URL=https://api.bisttrading.com
VITE_WS_BASE_URL=wss://api.bisttrading.com
VITE_API_TIMEOUT=30000
VITE_WS_RECONNECT_DELAY=3000
VITE_WS_MAX_RECONNECT_ATTEMPTS=5
```

## ✅ Build Verification

Build completed successfully:
```bash
✓ 3192 modules transformed
✓ built in 2.16s

Output:
- dist/index.html (0.68 kB)
- dist/assets/index-*.css (3.85 kB)
- dist/assets/vendor-*.js (89.82 kB)
- dist/assets/ui-*.js (495.55 kB)
- dist/assets/index-*.js (237.68 kB)
```

Code splitting working:
- vendor chunk (React, React Router DOM)
- ui chunk (Ant Design, Icons)
- state chunk (Zustand, TanStack Query)
- charts chunk (Lightweight Charts - will be used when implemented)

---

**Status**: ✅ Core frontend infrastructure complete and ready for feature development!

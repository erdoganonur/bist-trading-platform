import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import type { TickData, OrderBookData, TradeData } from '@services/websocket/websocket.types';

// Auth Store
interface AuthState {
  isAuthenticated: boolean;
  user: {
    userId: string;
    username: string;
    authorities: string[];
  } | null;
  accessToken: string | null;
  refreshToken: string | null;
  setAuth: (data: { accessToken: string; refreshToken: string; userId: string; username: string; authorities: string[] }) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  devtools(
    persist(
      (set) => ({
        isAuthenticated: false,
        user: null,
        accessToken: null,
        refreshToken: null,
        setAuth: (data) => set({
          isAuthenticated: true,
          user: {
            userId: data.userId,
            username: data.username,
            authorities: data.authorities,
          },
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        }),
        clearAuth: () => set({
          isAuthenticated: false,
          user: null,
          accessToken: null,
          refreshToken: null,
        }),
      }),
      {
        name: 'auth-storage',
        partialize: (state) => ({
          isAuthenticated: state.isAuthenticated,
          user: state.user,
          accessToken: state.accessToken,
          refreshToken: state.refreshToken,
        }),
      }
    )
  )
);

// Market Data Store
interface MarketDataState {
  ticks: Map<string, TickData>;
  orderBooks: Map<string, OrderBookData>;
  trades: Map<string, TradeData[]>;
  selectedSymbol: string | null;
  watchlist: string[];
  setTick: (symbol: string, tick: TickData) => void;
  setOrderBook: (symbol: string, orderBook: OrderBookData) => void;
  addTrade: (symbol: string, trade: TradeData) => void;
  setSelectedSymbol: (symbol: string | null) => void;
  addToWatchlist: (symbol: string) => void;
  removeFromWatchlist: (symbol: string) => void;
  clearMarketData: () => void;
}

export const useMarketDataStore = create<MarketDataState>()(
  devtools(
    persist(
      (set) => ({
        ticks: new Map(),
        orderBooks: new Map(),
        trades: new Map(),
        selectedSymbol: null,
        watchlist: [],
        setTick: (symbol, tick) => set((state) => {
          const newTicks = new Map(state.ticks);
          newTicks.set(symbol, tick);
          return { ticks: newTicks };
        }),
        setOrderBook: (symbol, orderBook) => set((state) => {
          const newOrderBooks = new Map(state.orderBooks);
          newOrderBooks.set(symbol, orderBook);
          return { orderBooks: newOrderBooks };
        }),
        addTrade: (symbol, trade) => set((state) => {
          const newTrades = new Map(state.trades);
          const symbolTrades = newTrades.get(symbol) || [];
          // Keep only last 100 trades per symbol
          const updatedTrades = [trade, ...symbolTrades].slice(0, 100);
          newTrades.set(symbol, updatedTrades);
          return { trades: newTrades };
        }),
        setSelectedSymbol: (symbol) => set({ selectedSymbol: symbol }),
        addToWatchlist: (symbol) => set((state) => ({
          watchlist: state.watchlist.includes(symbol)
            ? state.watchlist
            : [...state.watchlist, symbol]
        })),
        removeFromWatchlist: (symbol) => set((state) => ({
          watchlist: state.watchlist.filter(s => s !== symbol)
        })),
        clearMarketData: () => set({
          ticks: new Map(),
          orderBooks: new Map(),
          trades: new Map(),
        }),
      }),
      {
        name: 'market-data-storage',
        partialize: (state) => ({
          watchlist: state.watchlist,
          selectedSymbol: state.selectedSymbol,
        }),
      }
    )
  )
);

// WebSocket Connection Store
interface WebSocketState {
  isConnected: boolean;
  isConnecting: boolean;
  error: string | null;
  setConnected: (connected: boolean) => void;
  setConnecting: (connecting: boolean) => void;
  setError: (error: string | null) => void;
}

export const useWebSocketStore = create<WebSocketState>()(
  devtools((set) => ({
    isConnected: false,
    isConnecting: false,
    error: null,
    setConnected: (connected) => set({ isConnected: connected }),
    setConnecting: (connecting) => set({ isConnecting: connecting }),
    setError: (error) => set({ error }),
  }))
);

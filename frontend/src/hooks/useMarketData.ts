import { useEffect } from 'react';
import { useMarketDataStore } from '@/app/store';
import { useWebSocket } from './useWebSocket';
import type { TickData, OrderBookData, TradeData } from '@services/websocket/websocket.types';

export const useMarketData = (symbol?: string) => {
  const { subscribe, subscribeTick, unsubscribeTick, subscribeOrderBook, unsubscribeOrderBook, subscribeTrade, unsubscribeTrade } = useWebSocket();
  const { ticks, orderBooks, trades, setTick, setOrderBook, addTrade } = useMarketDataStore();

  useEffect(() => {
    if (!symbol) return;

    // Subscribe to tick data
    const unsubscribeTicks = subscribe('TICK', (data: TickData) => {
      if (data.symbol === symbol) {
        setTick(symbol, data);
      }
    });

    // Subscribe to order book
    const unsubscribeBooks = subscribe('ORDER_BOOK', (data: OrderBookData) => {
      if (data.symbol === symbol) {
        setOrderBook(symbol, data);
      }
    });

    // Subscribe to trades
    const unsubscribeTrades = subscribe('TRADE', (data: TradeData) => {
      if (data.symbol === symbol) {
        addTrade(symbol, data);
      }
    });

    // Request subscriptions
    subscribeTick(symbol);
    subscribeOrderBook(symbol);
    subscribeTrade(symbol);

    return () => {
      unsubscribeTicks();
      unsubscribeBooks();
      unsubscribeTrades();
      unsubscribeTick(symbol);
      unsubscribeOrderBook(symbol);
      unsubscribeTrade(symbol);
    };
  }, [symbol, subscribe, subscribeTick, unsubscribeTick, subscribeOrderBook, unsubscribeOrderBook, subscribeTrade, unsubscribeTrade, setTick, setOrderBook, addTrade]);

  const tick = symbol ? ticks.get(symbol) : undefined;
  const orderBook = symbol ? orderBooks.get(symbol) : undefined;
  const symbolTrades = symbol ? trades.get(symbol) || [] : [];

  return {
    tick,
    orderBook,
    trades: symbolTrades,
  };
};

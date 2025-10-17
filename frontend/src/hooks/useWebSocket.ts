import { useEffect, useCallback } from 'react';
import { wsService } from '@services/websocket/websocket.service';
import { useAuthStore } from '@/app/store';
import { useWebSocketStore } from '@/app/store';
import type { MessageHandler } from '@services/websocket/websocket.types';

export const useWebSocket = () => {
  const { accessToken, isAuthenticated } = useAuthStore();
  const { isConnected, setConnected, setConnecting, setError } = useWebSocketStore();

  const connect = useCallback(async () => {
    if (!accessToken || !isAuthenticated) {
      console.warn('[useWebSocket] No access token available');
      return;
    }

    if (isConnected) {
      console.log('[useWebSocket] Already connected');
      return;
    }

    try {
      setConnecting(true);
      setError(null);
      await wsService.connect(accessToken);
      setConnected(true);
      console.log('[useWebSocket] Connection established');
    } catch (error) {
      console.error('[useWebSocket] Connection failed:', error);
      setError(error instanceof Error ? error.message : 'Connection failed');
      setConnected(false);
    } finally {
      setConnecting(false);
    }
  }, [accessToken, isAuthenticated, isConnected, setConnected, setConnecting, setError]);

  const disconnect = useCallback(() => {
    wsService.disconnect();
    setConnected(false);
  }, [setConnected]);

  const subscribe = useCallback((channel: string, handler: MessageHandler) => {
    return wsService.subscribe(channel, handler);
  }, []);

  const subscribeTick = useCallback((symbol: string) => {
    wsService.subscribeTick(symbol);
  }, []);

  const unsubscribeTick = useCallback((symbol: string) => {
    wsService.unsubscribeTick(symbol);
  }, []);

  const subscribeOrderBook = useCallback((symbol: string) => {
    wsService.subscribeOrderBook(symbol);
  }, []);

  const unsubscribeOrderBook = useCallback((symbol: string) => {
    wsService.unsubscribeOrderBook(symbol);
  }, []);

  const subscribeTrade = useCallback((symbol: string) => {
    wsService.subscribeTrade(symbol);
  }, []);

  const unsubscribeTrade = useCallback((symbol: string) => {
    wsService.unsubscribeTrade(symbol);
  }, []);

  // Auto-connect when authenticated
  useEffect(() => {
    if (isAuthenticated && accessToken && !isConnected) {
      connect();
    }
  }, [isAuthenticated, accessToken, isConnected, connect]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    isConnected,
    connect,
    disconnect,
    subscribe,
    subscribeTick,
    unsubscribeTick,
    subscribeOrderBook,
    unsubscribeOrderBook,
    subscribeTrade,
    unsubscribeTrade,
  };
};

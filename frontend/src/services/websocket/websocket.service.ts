import type { WebSocketMessage, MessageHandler, TickData, OrderBookData, TradeData } from './websocket.types';

class WebSocketService {
  private ws: WebSocket | null = null;
  private subscribers = new Map<string, Set<MessageHandler>>();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private heartbeatInterval: number | null = null;
  private authenticated = false;

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const wsUrl = `${import.meta.env.VITE_WS_BASE_URL}/api/v1/broker/websocket`;

      console.log('[WebSocket] Connecting to:', wsUrl);
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log('[WebSocket] Connected');
        this.reconnectAttempts = 0;
        this.sendAuthMessage(token);
        this.startHeartbeat();
        resolve();
      };

      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (error) {
          console.error('[WebSocket] Message parse error:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('[WebSocket] Error:', error);
        reject(error);
      };

      this.ws.onclose = () => {
        console.warn('[WebSocket] Connection closed');
        this.authenticated = false;
        this.stopHeartbeat();
        this.attemptReconnect(token);
      };
    });
  }

  private sendAuthMessage(token: string) {
    this.send({
      type: 'AUTH',
      data: { token }
    });
  }

  private handleMessage(message: WebSocketMessage) {
    switch (message.type) {
      case 'AUTH_SUCCESS':
        console.log('[WebSocket] Authenticated');
        this.authenticated = true;
        break;

      case 'AUTH_FAILURE':
        console.error('[WebSocket] Authentication failed:', message.error);
        this.disconnect();
        break;

      case 'PONG':
        console.debug('[WebSocket] Received PONG');
        break;

      case 'TICK':
        this.notifySubscribers('TICK', message.data as TickData);
        break;

      case 'ORDER_BOOK':
        this.notifySubscribers('ORDER_BOOK', message.data as OrderBookData);
        break;

      case 'TRADE':
        this.notifySubscribers('TRADE', message.data as TradeData);
        break;

      case 'ERROR':
        console.error('[WebSocket] Server error:', message.error);
        break;

      default:
        console.debug('[WebSocket] Unhandled message type:', message.type);
    }
  }

  private startHeartbeat() {
    this.heartbeatInterval = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'PING' });
      }
    }, 30000); // 30 seconds
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private attemptReconnect(token: string) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`[WebSocket] Reconnecting (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);

      setTimeout(() => {
        this.connect(token).catch(err => {
          console.error('[WebSocket] Reconnect failed:', err);
        });
      }, this.reconnectDelay * this.reconnectAttempts);
    } else {
      console.error('[WebSocket] Max reconnect attempts reached');
    }
  }

  subscribe(channel: string, handler: MessageHandler) {
    if (!this.subscribers.has(channel)) {
      this.subscribers.set(channel, new Set());
    }
    this.subscribers.get(channel)!.add(handler);

    // Return unsubscribe function
    return () => {
      this.subscribers.get(channel)?.delete(handler);
    };
  }

  private notifySubscribers(channel: string, data: any) {
    this.subscribers.get(channel)?.forEach(handler => {
      try {
        handler(data);
      } catch (error) {
        console.error('[WebSocket] Handler error:', error);
      }
    });
  }

  subscribeTick(symbol: string) {
    if (!this.authenticated) {
      console.warn('[WebSocket] Not authenticated');
      return;
    }
    this.send({
      type: 'SUBSCRIBE',
      channel: 'TICK',
      data: { symbol }
    });
  }

  unsubscribeTick(symbol: string) {
    this.send({
      type: 'UNSUBSCRIBE',
      channel: 'TICK',
      data: { symbol }
    });
  }

  subscribeOrderBook(symbol: string) {
    if (!this.authenticated) {
      console.warn('[WebSocket] Not authenticated');
      return;
    }
    this.send({
      type: 'SUBSCRIBE',
      channel: 'ORDER_BOOK',
      data: { symbol }
    });
  }

  unsubscribeOrderBook(symbol: string) {
    this.send({
      type: 'UNSUBSCRIBE',
      channel: 'ORDER_BOOK',
      data: { symbol }
    });
  }

  subscribeTrade(symbol: string) {
    if (!this.authenticated) {
      console.warn('[WebSocket] Not authenticated');
      return;
    }
    this.send({
      type: 'SUBSCRIBE',
      channel: 'TRADE',
      data: { symbol }
    });
  }

  unsubscribeTrade(symbol: string) {
    this.send({
      type: 'UNSUBSCRIBE',
      channel: 'TRADE',
      data: { symbol }
    });
  }

  private send(message: WebSocketMessage) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.warn('[WebSocket] Cannot send, not connected');
    }
  }

  disconnect() {
    console.log('[WebSocket] Disconnecting');
    this.stopHeartbeat();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
    this.authenticated = false;
    this.subscribers.clear();
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN && this.authenticated;
  }
}

export const wsService = new WebSocketService();

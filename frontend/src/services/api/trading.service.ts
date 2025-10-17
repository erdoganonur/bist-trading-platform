import { axiosInstance } from './axios.config';

export type OrderSide = 'BUY' | 'SELL';
export type OrderType = 'LIMIT' | 'MARKET' | 'STOP_LIMIT' | 'STOP_MARKET';
export type OrderStatus = 'PENDING' | 'OPEN' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED';
export type TimeInForce = 'DAY' | 'GTC' | 'IOC' | 'FOK';

export interface CreateOrderRequest {
  symbol: string;
  side: OrderSide;
  orderType: OrderType;
  quantity: number;
  price?: number;
  stopPrice?: number;
  timeInForce?: TimeInForce;
}

export interface Order {
  orderId: string;
  symbol: string;
  side: OrderSide;
  orderType: OrderType;
  orderStatus: OrderStatus;
  quantity: number;
  filledQuantity: number;
  price?: number;
  stopPrice?: number;
  averageFillPrice?: number;
  timeInForce: TimeInForce;
  createdAt: string;
  updatedAt: string;
  executions: OrderExecution[];
}

export interface OrderExecution {
  executionId: string;
  tradeId: string;
  executionPrice: number;
  executionQuantity: number;
  commission: number;
  tax: number;
  executionTime: string;
}

export interface Portfolio {
  positions: PortfolioPosition[];
  totalValue: number;
  totalCost: number;
  totalPnl: number;
  totalPnlPercent: number;
  dayPnl: number;
  dayPnlPercent: number;
}

export interface PortfolioPosition {
  symbol: string;
  symbolName: string;
  quantity: number;
  averageCost: number;
  currentPrice: number;
  marketValue: number;
  unrealizedPnl: number;
  unrealizedPnlPercent: number;
  dayChange: number;
  dayChangePercent: number;
}

export const tradingService = {
  // Orders
  createOrder: (data: CreateOrderRequest) =>
    axiosInstance.post<Order>('/api/v1/broker/orders', data),

  getOrders: (params?: {
    symbol?: string;
    status?: OrderStatus;
    side?: OrderSide;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }) =>
    axiosInstance.get<{ content: Order[]; totalElements: number }>('/api/v1/broker/orders/history', { params }),

  getOrder: (orderId: string) =>
    axiosInstance.get<Order>(`/api/v1/broker/orders/${orderId}`),

  cancelOrder: (orderId: string) =>
    axiosInstance.delete(`/api/v1/broker/orders/${orderId}`),

  // Portfolio
  getPortfolio: () =>
    axiosInstance.get<Portfolio>('/api/v1/broker/portfolio'),
};

/**
 * AlgoLab Broker Integration Service
 * Provides authentication and broker operations for AlgoLab
 * Similar to Telegram Bot and CLI Client implementations
 */

import { axiosInstance } from './axios.config';

// ===================================================================
// AlgoLab Authentication Types
// ===================================================================

export interface AlgoLabLoginRequest {
  username: string;
  password: string;
}

export interface AlgoLabLoginResponse {
  success: boolean;
  smsSent: boolean;
  message: string;
}

export interface AlgoLabOTPRequest {
  otpCode: string;
}

export interface AlgoLabOTPResponse {
  success: boolean;
  authenticated: boolean;
  message: string;
  sessionExpiresAt?: string;
}

export interface AlgoLabAuthStatus {
  authenticated: boolean;
  username?: string;
  sessionId?: string;
  expiresAt?: string;
  websocketConnected?: boolean;
}

// ===================================================================
// AlgoLab Broker Types (from backend AlgoLabResponse)
// ===================================================================

export interface AlgoLabPosition {
  code: string;              // Symbol code (e.g., "AKBNK")
  totalstock: string;        // Total quantity (e.g., "365.000000")
  maliyet: string;           // Average cost price
  unitprice: string;         // Current market price
  profit: string;            // Profit/Loss amount
  totalvalue?: string;       // Total market value
  explanation?: string;      // Description
  type?: string;             // Position type
}

export interface AlgoLabPendingOrder {
  atpref: string;            // Order ID (AlgoLab reference)
  ticker: string;            // Symbol code (AlgoLab field name)
  buysell: string;           // "Alış" = BUY, "Satış" = SELL
  ordersize: string;         // Order quantity (lot)
  remainingsize: string;     // Remaining quantity
  price: string;             // Order price (0 for market orders)
  waitingprice: string;      // Actual price (used when price is 0)
  amount: string;            // Total amount
  status: string;            // Order status code
  description: string;       // Order status description (e.g., "İletildi")
  transactiontime: string;   // Transaction date
  timetransaction: string;   // Transaction time
  transactionId: string;     // Transaction ID
  equityStatusDescription: string; // Status description (e.g., "WAITING")
  timeinforce: string;       // Time in force
  fillunit: string;          // Filled units

  // Computed fields (for backward compatibility)
  symbol?: string;           // Computed from ticker
  direction?: string;        // Computed from buysell
  lot?: string;              // Computed from ordersize
  priceType?: string;        // Computed from price/waitingprice
  date?: string;             // Computed from transactiontime
  time?: string;             // Computed from timetransaction
}

export interface AlgoLabSendOrderRequest {
  symbol: string;
  direction: string;         // "0" = BUY, "1" = SELL
  priceType: string;         // "L" = Limit, "P" = Market
  price?: number;            // Required for Limit orders
  lot: number;               // Quantity in lots
  sms: boolean;              // SMS notification
  email: boolean;            // Email notification
  subAccount?: string;       // Sub-account (default: "0")
}

export interface AlgoLabSendOrderResponse {
  success: boolean;
  message?: string;
  content?: {
    orderId?: string;
    brokerOrderId?: string;
    status?: string;
  } | string;
}

export interface AlgoLabModifyOrderRequest {
  price?: number;
  lot?: number;
}

export interface AlgoLabCancelOrderResponse {
  success: boolean;
  message?: string;
}

// ===================================================================
// Market Data Types
// ===================================================================

export interface AlgoLabAccountInfo {
  accountNumber?: string;
  customerId?: string;
  status?: string;
  currency?: string;
  totalBalance?: number;
  availableBalance?: number;
  blockedBalance?: number;
  portfolioValue?: number;
}

// ===================================================================
// Broker Service
// ===================================================================

export const brokerService = {
  // =====================================================
  // Authentication
  // =====================================================

  /**
   * Step 1: Login to AlgoLab (sends OTP via SMS)
   */
  algoLabLogin: (data: AlgoLabLoginRequest) =>
    axiosInstance.post<AlgoLabLoginResponse>('/api/v1/broker/auth/login', data),

  /**
   * Step 2: Verify OTP code
   */
  algoLabVerifyOTP: (data: AlgoLabOTPRequest) =>
    axiosInstance.post<AlgoLabOTPResponse>('/api/v1/broker/auth/verify-otp', data),

  /**
   * Get current AlgoLab authentication status
   */
  algoLabAuthStatus: () =>
    axiosInstance.get<AlgoLabAuthStatus>('/api/v1/broker/auth/status'),

  /**
   * Logout from AlgoLab
   */
  algoLabLogout: () =>
    axiosInstance.post('/api/v1/broker/auth/logout'),

  // =====================================================
  // Broker Operations
  // =====================================================

  /**
   * Get account information
   */
  getAccountInfo: () =>
    axiosInstance.get<{ content: AlgoLabAccountInfo }>('/api/v1/broker/account'),

  /**
   * Get current positions from AlgoLab
   */
  getPositions: () =>
    axiosInstance.get<{ content: AlgoLabPosition[] }>('/api/v1/broker/positions'),

  /**
   * Get instant position snapshot
   */
  getInstantPosition: (subAccount: string = '0') =>
    axiosInstance.get<{ content: AlgoLabPosition[] }>('/api/v1/broker/instant-position', {
      params: { subAccount },
    }),

  // =====================================================
  // Order Management (AlgoLab Integration)
  // =====================================================

  /**
   * Get pending orders from AlgoLab (real-time data from broker)
   */
  getPendingOrders: (subAccount: string = '0') =>
    axiosInstance.get<{ success: boolean; count: number; orders: AlgoLabPendingOrder[]; message: string }>(
      '/api/v1/broker/orders/algolab-pending',
      { params: { subAccount } }
    ),

  /**
   * Send a new order to AlgoLab (⚠️ LIVE TRADING)
   */
  sendOrder: (data: AlgoLabSendOrderRequest) =>
    axiosInstance.post<AlgoLabSendOrderResponse>('/api/v1/broker/orders', data),

  /**
   * Modify an existing order
   */
  modifyOrder: (orderId: string, data: AlgoLabModifyOrderRequest) =>
    axiosInstance.put<AlgoLabSendOrderResponse>(`/api/v1/broker/orders/${orderId}`, data),

  /**
   * Cancel an existing order
   */
  cancelOrder: (orderId: string) =>
    axiosInstance.delete<AlgoLabCancelOrderResponse>(`/api/v1/broker/orders/${orderId}`),

  // =====================================================
  // Market Data (from AlgoLab)
  // =====================================================

  /**
   * Get current price for a symbol
   * Uses AlgoLab's InstantPosition to fetch real-time price
   */
  getCurrentPrice: async (symbol: string, subAccount: string = '0'): Promise<number | null> => {
    try {
      const response = await axiosInstance.get<{ content: AlgoLabPosition[] }>(
        '/api/v1/broker/instant-position',
        { params: { subAccount } }
      );

      const positions = response.data.content || [];
      const position = positions.find(
        (p) => p.code?.toUpperCase() === symbol.toUpperCase()
      );

      if (position && position.unitprice) {
        return parseFloat(position.unitprice);
      }

      return null;
    } catch (error) {
      console.error('Error fetching current price:', error);
      return null;
    }
  },

  // =====================================================
  // WebSocket
  // =====================================================

  /**
   * Get WebSocket connection status
   */
  getWebSocketStatus: () =>
    axiosInstance.get('/api/v1/broker/websocket/status'),

  /**
   * Subscribe to symbol tick data
   */
  subscribeToTick: (symbol: string) =>
    axiosInstance.post('/api/v1/broker/websocket/subscribe', {
      symbol,
      channel: 'tick',
    }),

  /**
   * Get tick stream for a symbol
   */
  getTickStream: (symbol: string, limit: number = 15) =>
    axiosInstance.get(`/api/v1/broker/websocket/stream/ticks/${symbol}`, {
      params: { limit },
    }),
};

// ===================================================================
// Cost Calculator Utility (Turkish Market Fees)
// Based on Telegram Bot's OrderCalculationService
// ===================================================================

export interface OrderCostEstimate {
  baseValue: number;         // Base order value (price * quantity)
  commission: number;        // 0.2% commission
  bsmv: number;              // 0.1% BSMV (on commission)
  bistFee: number;           // 0.003% BIST fee
  totalFees: number;         // Sum of all fees
  grandTotal: number;        // Final amount (baseValue ± totalFees)
  isBuy: boolean;
}

export const calculateOrderCost = (
  quantity: number,
  price: number,
  isBuy: boolean
): OrderCostEstimate => {
  const COMMISSION_RATE = 0.002;      // 0.2%
  const BSMV_RATE = 0.001;            // 0.1% on commission
  const BIST_FEE_RATE = 0.00003;      // 0.003%

  const baseValue = price * quantity;
  const commission = Math.round(baseValue * COMMISSION_RATE * 100) / 100;
  const bsmv = Math.round(commission * BSMV_RATE * 100) / 100;
  const bistFee = Math.round(baseValue * BIST_FEE_RATE * 100) / 100;
  const totalFees = commission + bsmv + bistFee;

  const grandTotal = isBuy
    ? baseValue + totalFees  // Buy: add fees
    : baseValue - totalFees; // Sell: subtract fees

  return {
    baseValue,
    commission,
    bsmv,
    bistFee,
    totalFees,
    grandTotal: Math.round(grandTotal * 100) / 100,
    isBuy,
  };
};

/**
 * Format cost estimate for display
 */
export const formatCostEstimate = (estimate: OrderCostEstimate): string => {
  const { baseValue, commission, bsmv, bistFee, totalFees, grandTotal, isBuy } = estimate;

  return `
💰 Maliyet Özeti

Toplam Değer: ₺${baseValue.toFixed(2)}
Komisyon: ₺${commission.toFixed(2)}
BSMV: ₺${bsmv.toFixed(2)}
Borsa Ücreti: ₺${bistFee.toFixed(2)}
━━━━━━━━━━━━━━━━━━━
${isBuy ? 'Ödenecek Tutar' : 'Alınacak Tutar'}: ₺${grandTotal.toFixed(2)}
  `.trim();
};

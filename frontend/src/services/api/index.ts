// Export all API services
export * from './auth.service';
export * from './broker.service';
export * from './market.service';
export * from './trading.service';

// Re-export services with convenient aliases
import { brokerService } from './broker.service';
import { marketService } from './market.service';
import { axiosInstance } from './axios.config';

// Broker API - Unified interface for widgets
export const brokerApi = {
  // Portfolio & Account
  async getPortfolio() {
    const response = await brokerService.getAccountInfo();
    const account = response.data.content;
    return {
      totalPortfolioValue: account.portfolioValue || 0,
      totalProfitLoss: 0, // Calculate from positions if needed
      availableBalance: account.availableBalance || 0,
    };
  },

  async getAccountInfo() {
    const response = await brokerService.getAccountInfo();
    return response.data.content;
  },

  // Positions
  async getPositions() {
    const response = await brokerService.getPositions();
    return response.data.content.map((pos: any) => ({
      symbol: pos.code,
      quantity: parseFloat(pos.totalstock),
      avgPrice: parseFloat(pos.maliyet),
      currentPrice: parseFloat(pos.unitprice),
      profitLoss: parseFloat(pos.profit),
    }));
  },

  // Orders
  async getPendingOrders() {
    const response = await brokerService.getPendingOrders();
    return response.data.orders.map((order: any) => ({
      id: order.atpref,
      symbol: order.ticker,
      side: order.buysell === 'Alış' ? 'BUY' : 'SELL',
      type: order.price === '0' ? 'MARKET' : 'LIMIT',
      price: parseFloat(order.waitingprice || order.price),
      quantity: parseFloat(order.ordersize) * 100,
      status: order.description,
      createdAt: `${order.transactiontime} ${order.timetransaction}`,
    }));
  },

  async placeOrder(data: {
    symbol: string;
    direction: '0' | '1';
    priceType: 'L' | 'P';
    price?: number;
    lot: number;
    smsNotification: 'H' | 'N';
    emailNotification: 'H' | 'N';
  }) {
    const response = await brokerService.sendOrder({
      symbol: data.symbol,
      direction: data.direction,
      priceType: data.priceType,
      price: data.price,
      lot: data.lot,
      sms: data.smsNotification === 'H',
      email: data.emailNotification === 'H',
    });
    return response.data;
  },

  async modifyOrder(orderId: string, data: { price: number; lot: number }) {
    const response = await brokerService.modifyOrder(orderId, data);
    return response.data;
  },

  async cancelOrder(orderId: string) {
    const response = await brokerService.cancelOrder(orderId);
    return response.data;
  },
};

// Symbol/Market Data API
export const symbolApi = {
  async searchSymbols(query: string) {
    const response = await marketService.searchSymbols(query);
    return response.data;
  },

  async getSymbol(symbol: string) {
    const response = await marketService.getSymbol(symbol);
    return response.data;
  },

  async getSymbols(params?: any) {
    const response = await marketService.getSymbols(params);
    return response.data.content;
  },
};

// Market Data API
export const marketDataApi = {
  async getWatchlist() {
    // This would typically come from a user preferences endpoint
    // For now, return a default watchlist
    const symbols = ['AKBNK', 'THYAO', 'GARAN', 'ISCTR', 'EREGL'];
    const promises = symbols.map((symbol) => symbolApi.getSymbol(symbol));
    const results = await Promise.all(promises);
    return results.map((data) => ({
      symbol: data.symbol,
      name: data.name,
      lastPrice: data.lastPrice,
      change: data.change,
      changePercent: data.changePercent,
      volume: data.volume,
      bid: data.lastPrice * 0.99,
      ask: data.lastPrice * 1.01,
    }));
  },

  async addToWatchlist(symbol: string) {
    // This would typically save to user preferences
    // For now, just return success
    return { success: true, symbol };
  },

  async removeFromWatchlist(symbol: string) {
    // This would typically remove from user preferences
    return { success: true, symbol };
  },

  async getOHLCV(symbol: string, timeframe: string) {
    const response = await axiosInstance.get(`/api/v1/market-data/ohlcv/${symbol}`, {
      params: { timeframe },
    });
    return response.data.content;
  },

  async getTickData(symbol: string) {
    const response = await brokerService.getTickStream(symbol);
    return response.data;
  },
};

// Export axios instance for custom requests
export { axiosInstance };

import { axiosInstance } from './axios.config';

export interface Symbol {
  id: string;
  symbol: string;
  name: string;
  nameTr: string;
  sector: string;
  market: string;
  isTradable: boolean;
  lastPrice: number;
  change: number;
  changePercent: number;
  volume: number;
  marketCap: number;
}

export interface SymbolSearchParams {
  query?: string;
  sector?: string;
  market?: string;
  isTradable?: boolean;
  page?: number;
  size?: number;
}

export interface Position {
  symbol: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  marketValue: number;
  unrealizedPnl: number;
  unrealizedPnlPercent: number;
}

export const marketService = {
  // Symbols
  getSymbols: (params?: SymbolSearchParams) =>
    axiosInstance.get<{ content: Symbol[]; totalElements: number }>('/api/v1/symbols', { params }),

  getSymbol: (symbol: string) =>
    axiosInstance.get<Symbol>(`/api/v1/symbols/${symbol}`),

  searchSymbols: (query: string) =>
    axiosInstance.get<Symbol[]>(`/api/v1/symbols/search`, { params: { query } }),

  // Positions
  getPositions: () =>
    axiosInstance.get<Position[]>('/api/v1/broker/positions'),

  getPosition: (symbol: string) =>
    axiosInstance.get<Position>(`/api/v1/broker/positions/${symbol}`),
};

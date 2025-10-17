export interface WebSocketMessage<T = any> {
  type: string;
  channel?: string;
  data?: T;
  error?: string;
  timestamp?: string;
}

export interface TickData {
  symbol: string;
  lastPrice: number;
  lastVolume: number;
  bidPrice: number;
  askPrice: number;
  bidSize: number;
  askSize: number;
  highPrice: number;
  lowPrice: number;
  openPrice: number;
  previousClose: number;
  totalVolume: number;
  change: number;
  changePercent: number;
  timestamp: string;
  marketStatus: string;
}

export interface OrderBookData {
  symbol: string;
  bids: OrderBookLevel[];
  asks: OrderBookLevel[];
  timestamp: string;
  sequence: number;
}

export interface OrderBookLevel {
  price: number;
  quantity: number;
  orderCount: number;
  side: 'BID' | 'ASK';
}

export interface TradeData {
  symbol: string;
  tradeId: string;
  price: number;
  quantity: number;
  side: 'BUY' | 'SELL';
  timestamp: string;
  isBuyerMaker: boolean;
  amount: number;
  sequence: number;
}

export type MessageHandler<T = any> = (data: T) => void;

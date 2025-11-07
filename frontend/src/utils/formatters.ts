/**
 * Format number as Turkish Lira currency
 */
export const formatCurrency = (value: number, decimals: number = 2): string => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};

/**
 * Format number as percentage
 */
export const formatPercent = (value: number, decimals: number = 2): string => {
  return `${value >= 0 ? '+' : ''}${value.toFixed(decimals)}%`;
};

/**
 * Format large numbers with K, M, B suffixes
 */
export const formatCompactNumber = (value: number): string => {
  if (value >= 1_000_000_000) {
    return `${(value / 1_000_000_000).toFixed(1)}B`;
  }
  if (value >= 1_000_000) {
    return `${(value / 1_000_000).toFixed(1)}M`;
  }
  if (value >= 1_000) {
    return `${(value / 1_000).toFixed(1)}K`;
  }
  return value.toFixed(0);
};

/**
 * Format number with thousands separator
 */
export const formatNumber = (value: number, decimals: number = 0): string => {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(value);
};

/**
 * Calculate order cost with fees (Turkish stock market)
 */
export interface OrderCost {
  baseValue: number;
  commission: number;
  bsmv: number;
  bistFee: number;
  total: number;
}

export const calculateOrderCost = (
  price: number,
  quantity: number,
  side: 'BUY' | 'SELL'
): OrderCost => {
  const baseValue = price * quantity;
  const commission = baseValue * 0.002; // 0.2%
  const bsmv = commission * 0.001; // 0.1% of commission
  const bistFee = baseValue * 0.00003; // 0.003%

  const totalFees = commission + bsmv + bistFee;
  const total = side === 'BUY' ? baseValue + totalFees : baseValue - totalFees;

  return {
    baseValue,
    commission,
    bsmv,
    bistFee,
    total,
  };
};

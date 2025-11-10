import React from 'react';
import {
  CTable,
  CTableHead,
  CTableBody,
  CTableRow,
  CTableHeaderCell,
  CTableDataCell,
  CBadge,
  CButton,
  CSpinner
} from '@coreui/react';
import { cilX, cilChart } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { Widget, PriceDisplay } from '@components/ui';
import { formatCurrency, formatPercent, formatNumber } from '@utils/formatters';
import { useQuery } from '@tanstack/react-query';
import { brokerApi } from '@services/api';
import { useMarketDataStore } from '@app/store';

interface Position {
  symbol: string;
  quantity: number;
  avgPrice: number;
  currentPrice: number;
  value: number;
  profitLoss: number;
  profitLossPercent: number;
}

export const PositionsWidget: React.FC = () => {
  const { ticks } = useMarketDataStore();

  const { data: positions, isLoading, refetch } = useQuery({
    queryKey: ['positions'],
    queryFn: brokerApi.getPositions,
    refetchInterval: 5000,
  });

  const enhancedPositions = React.useMemo(() => {
    if (!positions) return [];

    return positions.map((pos: any) => {
      const tick = ticks.get(pos.symbol);
      const currentPrice = tick?.lastPrice || pos.currentPrice || pos.avgPrice;
      const value = currentPrice * pos.quantity;
      const profitLoss = value - (pos.avgPrice * pos.quantity);
      const profitLossPercent = ((currentPrice - pos.avgPrice) / pos.avgPrice) * 100;

      return {
        ...pos,
        currentPrice,
        value,
        profitLoss,
        profitLossPercent,
      };
    });
  }, [positions, ticks]);

  return (
    <Widget
      title="Open Positions"
      icon={<CIcon icon={cilChart} />}
      onRefresh={refetch}
      extra={
        <CBadge color="info">
          {enhancedPositions.length} Position{enhancedPositions.length !== 1 ? 's' : ''}
        </CBadge>
      }
    >
      {isLoading ? (
        <div className="text-center py-4">
          <CSpinner color="primary" />
        </div>
      ) : enhancedPositions.length === 0 ? (
        <div className="text-center py-4 text-muted">
          <CIcon icon={cilChart} size="3xl" className="mb-3 opacity-25" />
          <p>No open positions</p>
        </div>
      ) : (
        <div className="table-responsive">
          <CTable small hover className="mb-0">
            <CTableHead>
              <CTableRow>
                <CTableHeaderCell>Symbol</CTableHeaderCell>
                <CTableHeaderCell className="text-end">Quantity</CTableHeaderCell>
                <CTableHeaderCell className="text-end">Avg Price</CTableHeaderCell>
                <CTableHeaderCell className="text-end">Current</CTableHeaderCell>
                <CTableHeaderCell className="text-end">Value</CTableHeaderCell>
                <CTableHeaderCell className="text-end">P&L</CTableHeaderCell>
                <CTableHeaderCell className="text-end">P&L %</CTableHeaderCell>
                <CTableHeaderCell className="text-end">Actions</CTableHeaderCell>
              </CTableRow>
            </CTableHead>
            <CTableBody>
              {enhancedPositions.map((pos: Position) => (
                <CTableRow key={pos.symbol}>
                  <CTableDataCell>
                    <span className="fw-semibold text-primary">{pos.symbol}</span>
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    {formatNumber(pos.quantity)}
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <span className="font-monospace">{formatCurrency(pos.avgPrice)}</span>
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <PriceDisplay
                      value={pos.currentPrice}
                      previousValue={pos.avgPrice}
                      suffix=" ₺"
                      animated
                    />
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <span className="font-monospace fw-semibold">{formatCurrency(pos.value)}</span>
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <span className={`font-monospace fw-semibold ${pos.profitLoss >= 0 ? 'text-success' : 'text-danger'}`}>
                      {pos.profitLoss >= 0 ? '+' : ''}{formatCurrency(pos.profitLoss)}
                    </span>
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <CBadge color={pos.profitLossPercent >= 0 ? 'success' : 'danger'} className="font-monospace">
                      {formatPercent(pos.profitLossPercent)}
                    </CBadge>
                  </CTableDataCell>
                  <CTableDataCell className="text-end">
                    <div className="d-flex gap-1 justify-content-end">
                      <CButton
                        color="primary"
                        variant="ghost"
                        size="sm"
                        title="View Chart"
                      >
                        <CIcon icon={cilChart} size="sm" />
                      </CButton>
                      <CButton
                        color="danger"
                        variant="ghost"
                        size="sm"
                        title="Close Position"
                      >
                        <CIcon icon={cilX} size="sm" />
                      </CButton>
                    </div>
                  </CTableDataCell>
                </CTableRow>
              ))}
            </CTableBody>
          </CTable>
        </div>
      )}
    </Widget>
  );
};

export default PositionsWidget;

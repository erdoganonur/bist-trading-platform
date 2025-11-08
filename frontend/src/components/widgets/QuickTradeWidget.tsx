import React, { useState, useEffect } from 'react';
import {
  CForm,
  CFormInput,
  CFormLabel,
  CAlert,
  CButton,
  CButtonGroup,
  CInputGroup,
  CInputGroupText,
} from '@coreui/react';
import { cilDollar, cilMagnifyingGlass } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { Widget, TradingButton } from '@components/ui';
import { calculateOrderCost, formatCurrency } from '@utils/formatters';
import { useMutation, useQuery } from '@tanstack/react-query';
import { brokerApi, symbolApi } from '@services/api';
import { useAlgoLabStore } from '@app/store';

interface QuickTradeForm {
  symbol: string;
  side: 'BUY' | 'SELL';
  priceType: 'LIMIT' | 'MARKET';
  price: number;
  lot: number;
}

export const QuickTradeWidget: React.FC = () => {
  const { isAuthenticated } = useAlgoLabStore();
  const [searchValue, setSearchValue] = useState('');
  const [symbol, setSymbol] = useState('');
  const [side, setSide] = useState<'BUY' | 'SELL'>('BUY');
  const [priceType, setPriceType] = useState<'LIMIT' | 'MARKET'>('LIMIT');
  const [price, setPrice] = useState<number>(0);
  const [lot, setLot] = useState<number>(1);
  const [cost, setCost] = useState<ReturnType<typeof calculateOrderCost> | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'danger'; message: string } | null>(null);

  // Search symbols
  const { data: symbols } = useQuery({
    queryKey: ['symbols', searchValue],
    queryFn: () => symbolApi.searchSymbols(searchValue),
    enabled: searchValue.length >= 2,
  });

  // Place order mutation
  const placeOrderMutation = useMutation({
    mutationFn: brokerApi.placeOrder,
    onSuccess: () => {
      setToast({ type: 'success', message: 'Order placed successfully!' });
      setSymbol('');
      setPrice(0);
      setLot(1);
      setCost(null);
    },
    onError: (error: any) => {
      setToast({ type: 'danger', message: error.message || 'Failed to place order' });
    },
  });

  // Calculate cost when values change
  useEffect(() => {
    if (price > 0 && lot > 0) {
      const quantity = lot * 100; // 1 lot = 100 shares
      const calculatedCost = calculateOrderCost(price, quantity, side);
      setCost(calculatedCost);
    } else {
      setCost(null);
    }
  }, [price, lot, side]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!symbol || lot <= 0 || (priceType === 'LIMIT' && price <= 0)) {
      setToast({ type: 'danger', message: 'Please fill in all required fields' });
      return;
    }

    const quantity = lot * 100;
    placeOrderMutation.mutate({
      symbol,
      direction: side === 'BUY' ? '0' : '1',
      priceType: priceType === 'LIMIT' ? 'L' : 'P',
      price: priceType === 'LIMIT' ? price : undefined,
      lot: quantity,
      smsNotification: 'H',
      emailNotification: 'H',
    });
  };

  return (
    <Widget
      title="Quick Trade"
      icon={<CIcon icon={cilDollar} />}
      className="h-full"
    >
      {!isAuthenticated && (
        <CAlert color="warning" className="mb-3 d-flex align-items-center">
          <div>
            <strong>AlgoLab Login Required</strong>
            <div className="small">Please login to AlgoLab using the status button in the header to place orders.</div>
          </div>
        </CAlert>
      )}

      {toast && (
        <CAlert
          color={toast.type}
          dismissible
          onClose={() => setToast(null)}
          className="mb-3"
        >
          {toast.message}
        </CAlert>
      )}

      <CForm onSubmit={handleSubmit}>
        {/* Symbol Search */}
        <div className="mb-3">
          <CFormLabel>Symbol</CFormLabel>
          <CInputGroup>
            <CFormInput
              type="text"
              placeholder="Search symbol (e.g., AKBNK)"
              value={symbol}
              onChange={(e) => {
                setSymbol(e.target.value);
                setSearchValue(e.target.value);
              }}
              list="symbols-datalist"
              disabled={!isAuthenticated}
              required
            />
            <CInputGroupText>
              <CIcon icon={cilMagnifyingGlass} />
            </CInputGroupText>
          </CInputGroup>
          <datalist id="symbols-datalist">
            {symbols?.map((s: any) => (
              <option key={s.symbol} value={s.symbol}>
                {s.name}
              </option>
            ))}
          </datalist>
        </div>

        {/* Buy/Sell Toggle */}
        <div className="mb-3">
          <CFormLabel>Side</CFormLabel>
          <CButtonGroup role="group" className="w-100">
            <CButton
              color={side === 'BUY' ? 'success' : 'secondary'}
              variant={side === 'BUY' ? 'outline' : 'ghost'}
              onClick={() => setSide('BUY')}
              disabled={!isAuthenticated}
              className="fw-semibold"
            >
              BUY
            </CButton>
            <CButton
              color={side === 'SELL' ? 'danger' : 'secondary'}
              variant={side === 'SELL' ? 'outline' : 'ghost'}
              onClick={() => setSide('SELL')}
              disabled={!isAuthenticated}
              className="fw-semibold"
            >
              SELL
            </CButton>
          </CButtonGroup>
        </div>

        {/* Price Type */}
        <div className="mb-3">
          <CFormLabel>Order Type</CFormLabel>
          <CButtonGroup role="group" className="w-100">
            <CButton
              color={priceType === 'LIMIT' ? 'primary' : 'secondary'}
              variant={priceType === 'LIMIT' ? 'outline' : 'ghost'}
              onClick={() => setPriceType('LIMIT')}
              disabled={!isAuthenticated}
            >
              Limit
            </CButton>
            <CButton
              color={priceType === 'MARKET' ? 'primary' : 'secondary'}
              variant={priceType === 'MARKET' ? 'outline' : 'ghost'}
              onClick={() => setPriceType('MARKET')}
              disabled={!isAuthenticated}
            >
              Market
            </CButton>
          </CButtonGroup>
        </div>

        {/* Price Input (only for LIMIT) */}
        {priceType === 'LIMIT' && (
          <div className="mb-3">
            <CFormLabel>Price (₺)</CFormLabel>
            <CFormInput
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={price || ''}
              onChange={(e) => setPrice(parseFloat(e.target.value) || 0)}
              disabled={!isAuthenticated}
              required
            />
          </div>
        )}

        {/* Quantity (Lot) */}
        <div className="mb-3">
          <CFormLabel>
            Quantity (Lot)
            <small className="text-muted ms-2">1 lot = 100 shares</small>
          </CFormLabel>
          <CInputGroup>
            <CFormInput
              type="number"
              step="1"
              min="1"
              placeholder="1"
              value={lot || ''}
              onChange={(e) => setLot(parseInt(e.target.value) || 1)}
              disabled={!isAuthenticated}
              required
            />
            <CInputGroupText>lot</CInputGroupText>
          </CInputGroup>
        </div>

        {/* Cost Calculation */}
        {cost && (
          <div className="bg-light p-3 rounded mb-3">
            <div className="small">
              <div className="d-flex justify-content-between mb-1">
                <span className="text-muted">Base Value:</span>
                <span className="font-monospace">{formatCurrency(cost.baseValue)}</span>
              </div>
              <div className="d-flex justify-content-between mb-1">
                <span className="text-muted">Commission (0.2%):</span>
                <span className="font-monospace">{formatCurrency(cost.commission)}</span>
              </div>
              <div className="d-flex justify-content-between mb-1">
                <span className="text-muted">BSMV Tax:</span>
                <span className="font-monospace">{formatCurrency(cost.bsmv)}</span>
              </div>
              <div className="d-flex justify-content-between mb-2">
                <span className="text-muted">BIST Fee:</span>
                <span className="font-monospace">{formatCurrency(cost.bistFee)}</span>
              </div>
              <hr className="my-2" />
              <div className="d-flex justify-content-between fw-semibold">
                <span>Total {side === 'BUY' ? 'Cost' : 'Proceeds'}:</span>
                <span className={`font-monospace ${side === 'BUY' ? 'text-danger' : 'text-success'}`}>
                  {formatCurrency(cost.total)}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Submit Button */}
        <div className="d-grid">
          <TradingButton
            action={side.toLowerCase() as 'buy' | 'sell'}
            htmlType="submit"
            size="large"
            block
            loading={placeOrderMutation.isPending}
            disabled={!isAuthenticated}
          >
            {side === 'BUY' ? 'Place Buy Order' : 'Place Sell Order'}
          </TradingButton>
        </div>
      </CForm>
    </Widget>
  );
};

export default QuickTradeWidget;

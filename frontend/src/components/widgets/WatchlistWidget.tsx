import React, { useState } from 'react';
import {
  CTable,
  CTableHead,
  CTableBody,
  CTableRow,
  CTableHeaderCell,
  CTableDataCell,
  CBadge,
  CButton,
  CSpinner,
  CModal,
  CModalHeader,
  CModalTitle,
  CModalBody,
  CModalFooter,
  CForm,
  CFormLabel,
  CFormInput,
  CInputGroup,
  CInputGroupText,
} from '@coreui/react';
import { cilStar, cilPlus, cilTrash, cilMagnifyingGlass } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { Widget, PriceDisplay } from '@components/ui';
import { formatCurrency, formatPercent, formatCompactNumber } from '@utils/formatters';
import { useMarketDataStore } from '@app/store';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { marketDataApi } from '@services/api';

interface WatchlistItem {
  symbol: string;
  name?: string;
  lastPrice: number;
  change: number;
  changePercent: number;
  volume: number;
  bid: number;
  ask: number;
}

export const WatchlistWidget: React.FC = () => {
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [searchSymbol, setSearchSymbol] = useState('');
  const { ticks } = useMarketDataStore();
  const queryClient = useQueryClient();

  const { data: watchlist, isLoading } = useQuery({
    queryKey: ['watchlist'],
    queryFn: marketDataApi.getWatchlist,
  });

  const addToWatchlistMutation = useMutation({
    mutationFn: marketDataApi.addToWatchlist,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['watchlist'] });
      setAddModalOpen(false);
      setSearchSymbol('');
    },
  });

  const removeFromWatchlistMutation = useMutation({
    mutationFn: marketDataApi.removeFromWatchlist,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['watchlist'] });
    },
  });

  const enhancedWatchlist = React.useMemo(() => {
    if (!watchlist) return [];

    return watchlist.map((item: any) => {
      const tick = ticks.get(item.symbol);
      if (tick) {
        return {
          ...item,
          lastPrice: tick.lastPrice || item.lastPrice,
          change: tick.change || item.change,
          changePercent: tick.changePercent || item.changePercent,
          volume: tick.totalVolume || item.volume,
          bid: tick.bidPrice || item.bid,
          ask: tick.askPrice || item.ask,
        };
      }
      return item;
    });
  }, [watchlist, ticks]);

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchSymbol.trim()) {
      addToWatchlistMutation.mutate(searchSymbol.toUpperCase());
    }
  };

  return (
    <>
      <Widget
        title="Watchlist"
        icon={<CIcon icon={cilStar} className="text-warning" />}
        extra={
          <CButton
            color="primary"
            size="sm"
            onClick={() => setAddModalOpen(true)}
          >
            <CIcon icon={cilPlus} className="me-1" />
            Add Symbol
          </CButton>
        }
      >
        {isLoading ? (
          <div className="text-center py-4">
            <CSpinner color="primary" />
          </div>
        ) : !enhancedWatchlist || enhancedWatchlist.length === 0 ? (
          <div className="text-center py-4 text-muted">
            <CIcon icon={cilStar} size="3xl" className="mb-3 opacity-25" />
            <p className="mb-3">No symbols in watchlist</p>
            <CButton
              color="primary"
              onClick={() => setAddModalOpen(true)}
            >
              <CIcon icon={cilPlus} className="me-1" />
              Add First Symbol
            </CButton>
          </div>
        ) : (
          <div className="table-responsive">
            <CTable small hover className="mb-0">
              <CTableHead>
                <CTableRow>
                  <CTableHeaderCell>Symbol</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Last Price</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Change</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Change %</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Volume</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Bid</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Ask</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Actions</CTableHeaderCell>
                </CTableRow>
              </CTableHead>
              <CTableBody>
                {enhancedWatchlist.map((item: WatchlistItem) => (
                  <CTableRow key={item.symbol}>
                    <CTableDataCell>
                      <div>
                        <div className="fw-semibold text-primary">{item.symbol}</div>
                        {item.name && (
                          <div className="small text-muted text-truncate" style={{ maxWidth: 150 }}>
                            {item.name}
                          </div>
                        )}
                      </div>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <PriceDisplay value={item.lastPrice} suffix=" ₺" animated />
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <span className={`font-monospace ${item.change >= 0 ? 'text-success' : 'text-danger'}`}>
                        {item.change >= 0 ? '+' : ''}{formatCurrency(item.change)}
                      </span>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <CBadge color={item.changePercent >= 0 ? 'success' : 'danger'} className="font-monospace">
                        {formatPercent(item.changePercent)}
                      </CBadge>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <span className="text-muted small">{formatCompactNumber(item.volume)}</span>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <span className="font-monospace small">{formatCurrency(item.bid)}</span>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <span className="font-monospace small">{formatCurrency(item.ask)}</span>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <CButton
                        color="danger"
                        variant="ghost"
                        size="sm"
                        onClick={() => removeFromWatchlistMutation.mutate(item.symbol)}
                        title="Remove from watchlist"
                      >
                        <CIcon icon={cilTrash} size="sm" />
                      </CButton>
                    </CTableDataCell>
                  </CTableRow>
                ))}
              </CTableBody>
            </CTable>
          </div>
        )}
      </Widget>

      {/* Add Symbol Modal */}
      <CModal
        visible={addModalOpen}
        onClose={() => {
          setAddModalOpen(false);
          setSearchSymbol('');
        }}
      >
        <CModalHeader>
          <CModalTitle>Add Symbol to Watchlist</CModalTitle>
        </CModalHeader>
        <CForm onSubmit={handleAddSubmit}>
          <CModalBody>
            <div className="mb-3">
              <CFormLabel>Symbol</CFormLabel>
              <CInputGroup>
                <CFormInput
                  type="text"
                  placeholder="Enter symbol (e.g., AKBNK)"
                  value={searchSymbol}
                  onChange={(e) => setSearchSymbol(e.target.value)}
                  required
                />
                <CInputGroupText>
                  <CIcon icon={cilMagnifyingGlass} />
                </CInputGroupText>
              </CInputGroup>
            </div>
          </CModalBody>
          <CModalFooter>
            <CButton
              color="secondary"
              onClick={() => {
                setAddModalOpen(false);
                setSearchSymbol('');
              }}
            >
              Cancel
            </CButton>
            <CButton
              color="primary"
              type="submit"
              disabled={addToWatchlistMutation.isPending}
            >
              {addToWatchlistMutation.isPending ? (
                <>
                  <CSpinner size="sm" className="me-2" />
                  Adding...
                </>
              ) : (
                'Add to Watchlist'
              )}
            </CButton>
          </CModalFooter>
        </CForm>
      </CModal>
    </>
  );
};

export default WatchlistWidget;

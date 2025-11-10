import { useEffect } from 'react';
import {
  CRow,
  CCol,
  CCard,
  CCardBody,
  CCardTitle,
  CBadge,
  CButton,
} from '@coreui/react';
import { cilArrowTop, cilArrowBottom, cilCheckCircle, cilXCircle, cilList } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { useNavigate } from 'react-router-dom';
import { useMarketDataStore, useWebSocketStore, useAlgoLabStore } from '@/app/store';
import { useAuth } from '@hooks/useAuth';
import { useWebSocket } from '@hooks/useWebSocket';
import { DashboardLayout } from '@components/layout';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isConnected } = useWebSocketStore();
  const { isAuthenticated: algoLabAuthenticated } = useAlgoLabStore();
  const { watchlist, ticks } = useMarketDataStore();
  const { subscribeTick, unsubscribeTick } = useWebSocket();

  useEffect(() => {
    // Subscribe to watchlist symbols
    watchlist.forEach(symbol => {
      subscribeTick(symbol);
    });

    return () => {
      watchlist.forEach(symbol => {
        unsubscribeTick(symbol);
      });
    };
  }, [watchlist, subscribeTick, unsubscribeTick]);

  return (
    <DashboardLayout>
      {/* Statistics Cards */}
      <CRow className="mb-4">
        <CCol sm={6} lg={3}>
          <CCard className="text-center">
            <CCardBody>
              <CCardTitle className="fs-6 text-muted">Portfolio Value</CCardTitle>
              <div className="fs-4 fw-bold text-success">₺112,893.00</div>
              <div className="small text-success">
                <CIcon icon={cilArrowTop} size="sm" /> +2.22%
              </div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol sm={6} lg={3}>
          <CCard className="text-center">
            <CCardBody>
              <CCardTitle className="fs-6 text-muted">Day P&L</CCardTitle>
              <div className="fs-4 fw-bold text-success">₺2,456.78</div>
              <div className="small text-success">
                <CIcon icon={cilArrowTop} size="sm" /> +2.22%
              </div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol sm={6} lg={3}>
          <CCard className="text-center">
            <CCardBody>
              <CCardTitle className="fs-6 text-muted">Day P&L %</CCardTitle>
              <div className="fs-4 fw-bold text-success">2.22%</div>
            </CCardBody>
          </CCard>
        </CCol>
        <CCol sm={6} lg={3}>
          <CCard
            className="text-center"
            style={{ cursor: algoLabAuthenticated ? 'pointer' : 'default' }}
            onClick={() => algoLabAuthenticated && navigate('/broker/pending-orders')}
          >
            <CCardBody>
              <CCardTitle className="fs-6 text-muted">Bekleyen Emirler</CCardTitle>
              <div className="fs-4 fw-bold text-primary">
                {algoLabAuthenticated ? '?' : '-'}
                {algoLabAuthenticated && (
                  <CIcon icon={cilList} size="sm" className="ms-2" />
                )}
              </div>
              {algoLabAuthenticated && (
                <CButton
                  color="link"
                  size="sm"
                  className="p-0 mt-2"
                >
                  Görüntüle →
                </CButton>
              )}
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Watchlist */}
      <CRow className="mb-4">
        <CCol xs={12}>
          <CCard>
            <CCardBody>
              <CCardTitle>Watchlist</CCardTitle>
              {watchlist.length === 0 ? (
                <p className="text-muted">No symbols in watchlist</p>
              ) : (
                <CRow className="g-3">
                  {watchlist.map(symbol => {
                    const tick = ticks.get(symbol);
                    const changeColor = tick && tick.change >= 0 ? 'success' : 'danger';
                    const ArrowIcon = tick && tick.change >= 0 ? cilArrowTop : cilArrowBottom;

                    return (
                      <CCol sm={6} md={4} lg={3} key={symbol}>
                        <CCard>
                          <CCardBody className="p-3">
                            <div className="fw-bold">{symbol}</div>
                            <div className="fs-4 fw-bold">
                              {tick ? `₺${tick.lastPrice.toFixed(2)}` : '--'}
                            </div>
                            <div className="d-flex gap-2">
                              <span className={`text-${changeColor}`}>
                                {tick && <CIcon icon={ArrowIcon} size="sm" />}
                                {tick ? `${tick.change.toFixed(2)}` : '--'}
                              </span>
                              <span className={`text-${changeColor}`}>
                                {tick ? `${tick.changePercent.toFixed(2)}%` : '--'}
                              </span>
                            </div>
                          </CCardBody>
                        </CCard>
                      </CCol>
                    );
                  })}
                </CRow>
              )}
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* Market Status */}
      <CRow>
        <CCol xs={12}>
          <CCard>
            <CCardBody>
              <CCardTitle className="d-flex align-items-center gap-2">
                <CIcon
                  icon={isConnected ? cilCheckCircle : cilXCircle}
                  size="lg"
                  className={isConnected ? 'text-success' : 'text-danger'}
                />
                Market Status
                <CBadge color={isConnected ? 'success' : 'danger'} className="ms-2">
                  {isConnected ? 'Connected' : 'Disconnected'}
                </CBadge>
              </CCardTitle>
              <p className="mb-0">
                {isConnected
                  ? 'WebSocket connected. Real-time market data streaming...'
                  : 'WebSocket disconnected. Attempting to reconnect...'}
              </p>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </DashboardLayout>
  );
};

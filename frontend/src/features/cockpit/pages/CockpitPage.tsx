import React from 'react';
import { CRow, CCol } from '@coreui/react';
import { DashboardLayout } from '@components/layout';
import {
  PortfolioWidget,
  QuickTradeWidget,
  PositionsWidget,
  PendingOrdersWidget,
  WatchlistWidget,
  ChartWidget,
} from '@components/widgets';

export const CockpitPage: React.FC = () => {
  return (
    <DashboardLayout>
      <div className="min-vh-100">
        {/* Portfolio Summary - Full Width */}
        <CRow className="mb-4">
          <CCol xs={12}>
            <PortfolioWidget />
          </CCol>
        </CRow>

        {/* Chart and Quick Trade - Side by Side */}
        <CRow className="mb-4">
          <CCol xs={12} lg={8}>
            <ChartWidget />
          </CCol>
          <CCol xs={12} lg={4}>
            <QuickTradeWidget />
          </CCol>
        </CRow>

        {/* Positions and Pending Orders - Side by Side */}
        <CRow className="mb-4">
          <CCol xs={12} lg={6}>
            <PositionsWidget />
          </CCol>
          <CCol xs={12} lg={6}>
            <PendingOrdersWidget />
          </CCol>
        </CRow>

        {/* Watchlist - Full Width */}
        <CRow className="mb-4">
          <CCol xs={12}>
            <WatchlistWidget />
          </CCol>
        </CRow>
      </div>
    </DashboardLayout>
  );
};

export default CockpitPage;

import React from 'react';
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
      <div className="h-full overflow-auto bg-gray-50">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 px-6 py-4">
          <h1 className="text-xl font-semibold text-gray-900">Trading Cockpit</h1>
        </div>

        {/* Main Grid Layout */}
        <div className="p-4 space-y-4">
          {/* Portfolio Summary - Full Width */}
          <div className="w-full">
            <PortfolioWidget />
          </div>

          {/* Chart and Quick Trade - Side by Side */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div className="lg:col-span-2">
              <ChartWidget />
            </div>
            <div className="lg:col-span-1">
              <QuickTradeWidget />
            </div>
          </div>

          {/* Positions and Pending Orders - Side by Side */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div>
              <PositionsWidget />
            </div>
            <div>
              <PendingOrdersWidget />
            </div>
          </div>

          {/* Watchlist - Full Width */}
          <div className="w-full">
            <WatchlistWidget />
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default CockpitPage;

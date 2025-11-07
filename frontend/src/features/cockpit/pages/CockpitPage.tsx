import React, { useState, useCallback } from 'react';
import { Button, Space, Dropdown } from 'antd';
import {
  LayoutOutlined,
  SaveOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { GridLayout } from '@components/layout';
import {
  PortfolioWidget,
  QuickTradeWidget,
  PositionsWidget,
  PendingOrdersWidget,
  WatchlistWidget,
  ChartWidget,
} from '@components/widgets';
import type { Layout as GridLayoutType } from 'react-grid-layout';

// Default layout configuration
const defaultLayout: GridLayoutType[] = [
  { i: 'portfolio', x: 0, y: 0, w: 12, h: 2, minH: 2, minW: 6 },
  { i: 'chart', x: 0, y: 2, w: 8, h: 8, minH: 6, minW: 6 },
  { i: 'quickTrade', x: 8, y: 2, w: 4, h: 8, minH: 6, minW: 3 },
  { i: 'positions', x: 0, y: 10, w: 6, h: 6, minH: 4, minW: 6 },
  { i: 'pendingOrders', x: 6, y: 10, w: 6, h: 6, minH: 4, minW: 6 },
  { i: 'watchlist', x: 0, y: 16, w: 12, h: 6, minH: 4, minW: 6 },
];

// Layout presets
const layoutPresets = {
  default: defaultLayout,
  trading: [
    { i: 'portfolio', x: 0, y: 0, w: 12, h: 2, minH: 2, minW: 6 },
    { i: 'quickTrade', x: 0, y: 2, w: 4, h: 10, minH: 6, minW: 3 },
    { i: 'chart', x: 4, y: 2, w: 8, h: 10, minH: 6, minW: 6 },
    { i: 'pendingOrders', x: 0, y: 12, w: 12, h: 5, minH: 4, minW: 6 },
    { i: 'positions', x: 0, y: 17, w: 12, h: 5, minH: 4, minW: 6 },
    { i: 'watchlist', x: 0, y: 22, w: 12, h: 5, minH: 4, minW: 6 },
  ],
  analysis: [
    { i: 'portfolio', x: 0, y: 0, w: 12, h: 2, minH: 2, minW: 6 },
    { i: 'chart', x: 0, y: 2, w: 9, h: 12, minH: 8, minW: 6 },
    { i: 'watchlist', x: 9, y: 2, w: 3, h: 12, minH: 6, minW: 3 },
    { i: 'positions', x: 0, y: 14, w: 6, h: 6, minH: 4, minW: 6 },
    { i: 'pendingOrders', x: 6, y: 14, w: 6, h: 6, minH: 4, minW: 6 },
    { i: 'quickTrade', x: 0, y: 20, w: 4, h: 6, minH: 6, minW: 3 },
  ],
};

export const CockpitPage: React.FC = () => {
  const [layout, setLayout] = useState<GridLayoutType[]>(() => {
    const saved = localStorage.getItem('cockpit-layout');
    return saved ? JSON.parse(saved) : defaultLayout;
  });

  const [isLocked, setIsLocked] = useState(false);

  const handleLayoutChange = useCallback((newLayout: GridLayoutType[]) => {
    setLayout(newLayout);
  }, []);

  const handleSaveLayout = () => {
    localStorage.setItem('cockpit-layout', JSON.stringify(layout));
    // message.success('Layout saved successfully');
  };

  const handleResetLayout = () => {
    setLayout(defaultLayout);
    localStorage.removeItem('cockpit-layout');
  };

  const handleLoadPreset = (preset: keyof typeof layoutPresets) => {
    setLayout(layoutPresets[preset]);
  };

  const layoutMenuItems: MenuProps['items'] = [
    {
      key: 'default',
      label: 'Default Layout',
      onClick: () => handleLoadPreset('default'),
    },
    {
      key: 'trading',
      label: 'Trading Focus',
      onClick: () => handleLoadPreset('trading'),
    },
    {
      key: 'analysis',
      label: 'Analysis Focus',
      onClick: () => handleLoadPreset('analysis'),
    },
    { type: 'divider' },
    {
      key: 'reset',
      label: 'Reset to Default',
      danger: true,
      onClick: handleResetLayout,
    },
  ];

  return (
    <div className="h-full overflow-auto">
      {/* Toolbar */}
      <div className="sticky top-0 z-10 bg-white border-b border-gray-200 px-4 py-3 mb-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800 m-0">
            Trading Cockpit
          </h1>

          <Space>
            <Button
              type={isLocked ? 'default' : 'primary'}
              icon={<LayoutOutlined />}
              onClick={() => setIsLocked(!isLocked)}
            >
              {isLocked ? 'Unlock Layout' : 'Lock Layout'}
            </Button>

            <Dropdown menu={{ items: layoutMenuItems }} placement="bottomRight">
              <Button icon={<LayoutOutlined />}>
                Layout Presets
              </Button>
            </Dropdown>

            <Button
              icon={<SaveOutlined />}
              onClick={handleSaveLayout}
              disabled={isLocked}
            >
              Save Layout
            </Button>

            <Button
              icon={<ReloadOutlined />}
              onClick={handleResetLayout}
            >
              Reset
            </Button>
          </Space>
        </div>
      </div>

      {/* Dashboard Grid */}
      <div className="px-4 pb-4">
        <GridLayout
          layout={layout}
          onLayoutChange={handleLayoutChange}
          isDraggable={!isLocked}
          isResizable={!isLocked}
        >
          <div key="portfolio">
            <PortfolioWidget />
          </div>

          <div key="chart">
            <ChartWidget />
          </div>

          <div key="quickTrade">
            <QuickTradeWidget />
          </div>

          <div key="positions">
            <PositionsWidget />
          </div>

          <div key="pendingOrders">
            <PendingOrdersWidget />
          </div>

          <div key="watchlist">
            <WatchlistWidget />
          </div>
        </GridLayout>
      </div>
    </div>
  );
};

export default CockpitPage;

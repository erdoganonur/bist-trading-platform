import React from 'react';
import { Table, Tag, Button, Space, Empty } from 'antd';
import { CloseOutlined, LineChartOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
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
    refetchInterval: 5000, // Refresh every 5 seconds
  });

  // Enhance positions with real-time prices
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

  const columns: ColumnsType<Position> = [
    {
      title: 'Symbol',
      dataIndex: 'symbol',
      key: 'symbol',
      fixed: 'left',
      width: 100,
      render: (symbol: string) => (
        <span className="font-semibold text-primary-600">{symbol}</span>
      ),
    },
    {
      title: 'Quantity',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 100,
      align: 'right',
      render: (qty: number) => formatNumber(qty),
    },
    {
      title: 'Avg Price',
      dataIndex: 'avgPrice',
      key: 'avgPrice',
      width: 120,
      align: 'right',
      render: (price: number) => (
        <span className="font-mono">{formatCurrency(price)}</span>
      ),
    },
    {
      title: 'Current Price',
      dataIndex: 'currentPrice',
      key: 'currentPrice',
      width: 120,
      align: 'right',
      render: (price: number, record) => (
        <PriceDisplay
          value={price}
          previousValue={record.avgPrice}
          suffix=" ₺"
          animated
        />
      ),
    },
    {
      title: 'Value',
      dataIndex: 'value',
      key: 'value',
      width: 120,
      align: 'right',
      render: (value: number) => (
        <span className="font-mono font-semibold">{formatCurrency(value)}</span>
      ),
    },
    {
      title: 'P&L',
      dataIndex: 'profitLoss',
      key: 'profitLoss',
      width: 120,
      align: 'right',
      render: (pl: number) => (
        <span className={`font-mono font-semibold ${pl >= 0 ? 'text-trading-profit' : 'text-trading-loss'}`}>
          {pl >= 0 ? '+' : ''}{formatCurrency(pl)}
        </span>
      ),
      sorter: (a, b) => a.profitLoss - b.profitLoss,
    },
    {
      title: 'P&L %',
      dataIndex: 'profitLossPercent',
      key: 'profitLossPercent',
      width: 100,
      align: 'right',
      render: (percent: number) => (
        <Tag color={percent >= 0 ? 'success' : 'error'} className="font-mono font-semibold">
          {formatPercent(percent)}
        </Tag>
      ),
      sorter: (a, b) => a.profitLossPercent - b.profitLossPercent,
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 120,
      render: () => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<LineChartOutlined />}
            title="View Chart"
          />
          <Button
            type="text"
            size="small"
            icon={<CloseOutlined />}
            danger
            title="Close Position"
          />
        </Space>
      ),
    },
  ];

  return (
    <Widget
      title="Open Positions"
      icon={<LineChartOutlined />}
      onRefresh={refetch}
      extra={
        <Tag color="blue">
          {enhancedPositions.length} Position{enhancedPositions.length !== 1 ? 's' : ''}
        </Tag>
      }
    >
      <Table
        columns={columns}
        dataSource={enhancedPositions}
        loading={isLoading}
        rowKey="symbol"
        size="small"
        scroll={{ x: 900 }}
        pagination={false}
        locale={{
          emptyText: (
            <Empty
              description="No open positions"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            />
          ),
        }}
      />
    </Widget>
  );
};

export default PositionsWidget;

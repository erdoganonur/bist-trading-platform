import React, { useState } from 'react';
import { Table, Button, Input, Space, Tag, Empty, Modal, Form } from 'antd';
import {
  StarFilled,
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
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

  // Enhance watchlist with real-time tick data
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

  const columns: ColumnsType<WatchlistItem> = [
    {
      title: 'Symbol',
      dataIndex: 'symbol',
      key: 'symbol',
      fixed: 'left',
      width: 100,
      render: (symbol: string, record) => (
        <div>
          <div className="font-semibold text-primary-600">{symbol}</div>
          {record.name && (
            <div className="text-xs text-gray-500 truncate">{record.name}</div>
          )}
        </div>
      ),
    },
    {
      title: 'Last Price',
      dataIndex: 'lastPrice',
      key: 'lastPrice',
      width: 120,
      align: 'right',
      render: (price: number) => (
        <PriceDisplay value={price} suffix=" ₺" animated />
      ),
    },
    {
      title: 'Change',
      dataIndex: 'change',
      key: 'change',
      width: 100,
      align: 'right',
      render: (change: number) => (
        <span className={`font-mono ${change >= 0 ? 'text-trading-profit' : 'text-trading-loss'}`}>
          {change >= 0 ? '+' : ''}{formatCurrency(change)}
        </span>
      ),
    },
    {
      title: 'Change %',
      dataIndex: 'changePercent',
      key: 'changePercent',
      width: 100,
      align: 'right',
      render: (percent: number) => (
        <Tag color={percent >= 0 ? 'success' : 'error'} className="font-mono">
          {formatPercent(percent)}
        </Tag>
      ),
      sorter: (a, b) => a.changePercent - b.changePercent,
    },
    {
      title: 'Volume',
      dataIndex: 'volume',
      key: 'volume',
      width: 100,
      align: 'right',
      render: (volume: number) => (
        <span className="text-gray-600">{formatCompactNumber(volume)}</span>
      ),
    },
    {
      title: 'Bid',
      dataIndex: 'bid',
      key: 'bid',
      width: 100,
      align: 'right',
      render: (bid: number) => (
        <span className="font-mono text-xs">{formatCurrency(bid)}</span>
      ),
    },
    {
      title: 'Ask',
      dataIndex: 'ask',
      key: 'ask',
      width: 100,
      align: 'right',
      render: (ask: number) => (
        <span className="font-mono text-xs">{formatCurrency(ask)}</span>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 80,
      render: (_, record) => (
        <Button
          type="text"
          size="small"
          icon={<DeleteOutlined />}
          danger
          onClick={() => removeFromWatchlistMutation.mutate(record.symbol)}
          title="Remove from watchlist"
        />
      ),
    },
  ];

  return (
    <>
      <Widget
        title="Watchlist"
        icon={<StarFilled className="text-yellow-500" />}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            Add Symbol
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={enhancedWatchlist}
          loading={isLoading}
          rowKey="symbol"
          size="small"
          scroll={{ x: 800 }}
          pagination={false}
          locale={{
            emptyText: (
              <Empty
                description="No symbols in watchlist"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setAddModalOpen(true)}
                >
                  Add First Symbol
                </Button>
              </Empty>
            ),
          }}
        />
      </Widget>

      {/* Add Symbol Modal */}
      <Modal
        title="Add Symbol to Watchlist"
        open={addModalOpen}
        onCancel={() => {
          setAddModalOpen(false);
          setSearchSymbol('');
        }}
        footer={null}
      >
        <Form
          onFinish={() => addToWatchlistMutation.mutate(searchSymbol.toUpperCase())}
        >
          <Form.Item label="Symbol" required>
            <Input
              size="large"
              placeholder="Enter symbol (e.g., AKBNK)"
              value={searchSymbol}
              onChange={(e) => setSearchSymbol(e.target.value)}
              suffix={<SearchOutlined />}
              onPressEnter={() => addToWatchlistMutation.mutate(searchSymbol.toUpperCase())}
            />
          </Form.Item>

          <Form.Item>
            <Space className="w-full justify-end">
              <Button onClick={() => setAddModalOpen(false)}>Cancel</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={addToWatchlistMutation.isPending}
              >
                Add to Watchlist
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default WatchlistWidget;

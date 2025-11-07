import React from 'react';
import { Row, Col, Spin } from 'antd';
import { DollarOutlined, RiseOutlined, FallOutlined, WalletOutlined } from '@ant-design/icons';
import { Widget, StatCard } from '@components/ui';
import { formatCurrency, formatPercent } from '@utils/formatters';
import { useQuery } from '@tanstack/react-query';
import { brokerApi } from '@services/api';

export const PortfolioWidget: React.FC = () => {
  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: brokerApi.getPortfolio,
    refetchInterval: 5000, // Refresh every 5 seconds
  });

  const { data: account } = useQuery({
    queryKey: ['account'],
    queryFn: brokerApi.getAccountInfo,
    refetchInterval: 10000,
  });

  if (isLoading) {
    return (
      <Widget title="Portfolio Summary">
        <div className="flex justify-center items-center h-32">
          <Spin size="large" />
        </div>
      </Widget>
    );
  }

  const totalValue = portfolio?.totalPortfolioValue || 0;
  const dayPL = portfolio?.totalProfitLoss || 0;
  const dayPLPercent = totalValue > 0 ? (dayPL / totalValue) * 100 : 0;
  const buyingPower = account?.availableBalance || 0;

  return (
    <Widget title="Portfolio Summary" icon={<DollarOutlined />}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="Portfolio Value"
            value={formatCurrency(totalValue)}
            icon={<WalletOutlined />}
            valueStyle={{ color: '#667eea' }}
          />
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="Day P&L"
            value={formatCurrency(dayPL)}
            trend={dayPL >= 0 ? 'up' : 'down'}
            trendValue={formatPercent(dayPLPercent)}
            icon={dayPL >= 0 ? <RiseOutlined /> : <FallOutlined />}
            valueStyle={{ color: dayPL >= 0 ? '#52c41a' : '#ff4d4f' }}
          />
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="Day P&L %"
            value={formatPercent(dayPLPercent)}
            trend={dayPLPercent >= 0 ? 'up' : 'down'}
            valueStyle={{ color: dayPLPercent >= 0 ? '#52c41a' : '#ff4d4f' }}
          />
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="Buying Power"
            value={formatCurrency(buyingPower)}
            icon={<DollarOutlined />}
            valueStyle={{ color: '#52c41a' }}
          />
        </Col>
      </Row>
    </Widget>
  );
};

export default PortfolioWidget;

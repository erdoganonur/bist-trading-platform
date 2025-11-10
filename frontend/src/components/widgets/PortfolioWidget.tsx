import React from 'react';
import { CRow, CCol, CAlert, CSpinner } from '@coreui/react';
import { cilDollar, cilArrowTop, cilArrowBottom, cilWallet } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { Widget, StatCard } from '@components/ui';
import { formatCurrency, formatPercent } from '@utils/formatters';
import { useQuery } from '@tanstack/react-query';
import { brokerApi } from '@services/api';
import { useAlgoLabStore } from '@app/store';

export const PortfolioWidget: React.FC = () => {
  const { isAuthenticated } = useAlgoLabStore();

  const { data: portfolio, isLoading, error } = useQuery({
    queryKey: ['portfolio'],
    queryFn: async () => {
      try {
        return await brokerApi.getPortfolio();
      } catch (err: any) {
        console.error('Portfolio fetch error:', err);
        throw err;
      }
    },
    refetchInterval: 5000,
    enabled: isAuthenticated,
    retry: false,
  });

  const { data: account } = useQuery({
    queryKey: ['account'],
    queryFn: async () => {
      try {
        return await brokerApi.getAccountInfo();
      } catch (err: any) {
        console.error('Account fetch error:', err);
        throw err;
      }
    },
    refetchInterval: 10000,
    enabled: isAuthenticated,
    retry: false,
  });

  if (!isAuthenticated) {
    return (
      <Widget title="Portfolio Summary" icon={<CIcon icon={cilDollar} />}>
        <CAlert color="info">
          <strong>AlgoLab Login Required</strong>
          <p className="mb-0 mt-1">Please login to AlgoLab to view your portfolio summary.</p>
        </CAlert>
      </Widget>
    );
  }

  if (isLoading) {
    return (
      <Widget title="Portfolio Summary">
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '128px' }}>
          <CSpinner color="primary" />
        </div>
      </Widget>
    );
  }

  if (error) {
    const errorMessage = (error as any)?.response?.data?.message ||
                        (error as any)?.message ||
                        'Failed to load portfolio data. Please try again later.';
    return (
      <Widget title="Portfolio Summary" icon={<CIcon icon={cilDollar} />}>
        <CAlert color="danger">
          <strong>Error Loading Portfolio</strong>
          <p className="mb-0 mt-1">{errorMessage}</p>
        </CAlert>
      </Widget>
    );
  }

  const totalValue = portfolio?.totalPortfolioValue || 0;
  const dayPL = portfolio?.totalProfitLoss || 0;
  const dayPLPercent = totalValue > 0 ? (dayPL / totalValue) * 100 : 0;
  const buyingPower = account?.availableBalance || 0;

  return (
    <Widget title="Portfolio Summary" icon={<CIcon icon={cilDollar} />}>
      <CRow className="g-3">
        <CCol xs={12} sm={6} lg={3}>
          <StatCard
            title="Portfolio Value"
            value={formatCurrency(totalValue)}
            icon={<CIcon icon={cilWallet} />}
            valueStyle={{ color: '#667eea' }}
          />
        </CCol>

        <CCol xs={12} sm={6} lg={3}>
          <StatCard
            title="Day P&L"
            value={formatCurrency(dayPL)}
            trend={dayPL >= 0 ? 'up' : 'down'}
            trendValue={formatPercent(dayPLPercent)}
            icon={dayPL >= 0 ? <CIcon icon={cilArrowTop} /> : <CIcon icon={cilArrowBottom} />}
            valueStyle={{ color: dayPL >= 0 ? '#52c41a' : '#ff4d4f' }}
          />
        </CCol>

        <CCol xs={12} sm={6} lg={3}>
          <StatCard
            title="Day P&L %"
            value={formatPercent(dayPLPercent)}
            trend={dayPLPercent >= 0 ? 'up' : 'down'}
            valueStyle={{ color: dayPLPercent >= 0 ? '#52c41a' : '#ff4d4f' }}
          />
        </CCol>

        <CCol xs={12} sm={6} lg={3}>
          <StatCard
            title="Buying Power"
            value={formatCurrency(buyingPower)}
            icon={<CIcon icon={cilDollar} />}
            valueStyle={{ color: '#52c41a' }}
          />
        </CCol>
      </CRow>
    </Widget>
  );
};

export default PortfolioWidget;

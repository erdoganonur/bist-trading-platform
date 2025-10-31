import { useEffect } from 'react';
import { Layout, Row, Col, Card, Statistic, Typography, Space, Button, Tag, Divider } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, WifiOutlined, DisconnectOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useMarketDataStore, useWebSocketStore, useAlgoLabStore } from '@/app/store';
import { useAuth } from '@hooks/useAuth';
import { useWebSocket } from '@hooks/useWebSocket';
import { AlgoLabStatusButton } from '@features/broker/components';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
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
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: '#001529',
        padding: '0 24px',
      }}>
        <Title level={3} style={{ color: 'white', margin: 0 }}>
          BIST Trading Platform
        </Title>
        <Space>
          <Tag
            icon={isConnected ? <WifiOutlined /> : <DisconnectOutlined />}
            color={isConnected ? 'success' : 'error'}
          >
            {isConnected ? 'Connected' : 'Disconnected'}
          </Tag>
          <Divider type="vertical" style={{ borderColor: 'rgba(255, 255, 255, 0.3)', height: '32px' }} />
          <AlgoLabStatusButton />
          <Divider type="vertical" style={{ borderColor: 'rgba(255, 255, 255, 0.3)', height: '32px' }} />
          <Text style={{ color: 'white' }}>
            Welcome, {user?.username}
          </Text>
          <Button onClick={logout}>
            Logout
          </Button>
        </Space>
      </Header>

      <Content style={{ padding: '24px' }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <Row gutter={16}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Portfolio Value"
                  value={112893}
                  precision={2}
                  valueStyle={{ color: '#3f8600' }}
                  prefix="₺"
                  suffix={<ArrowUpOutlined />}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Day P&L"
                  value={2456.78}
                  precision={2}
                  valueStyle={{ color: '#3f8600' }}
                  prefix="₺"
                  suffix={<ArrowUpOutlined />}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Day P&L %"
                  value={2.22}
                  precision={2}
                  valueStyle={{ color: '#3f8600' }}
                  suffix="%"
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card
                hoverable
                onClick={() => algoLabAuthenticated && navigate('/broker/pending-orders')}
                style={{ cursor: algoLabAuthenticated ? 'pointer' : 'default' }}
              >
                <Statistic
                  title="Bekleyen Emirler"
                  value={algoLabAuthenticated ? '?' : '-'}
                  valueStyle={{ color: '#1890ff' }}
                  suffix={
                    algoLabAuthenticated && (
                      <UnorderedListOutlined style={{ fontSize: '16px' }} />
                    )
                  }
                />
                {algoLabAuthenticated && (
                  <Button
                    type="link"
                    size="small"
                    style={{ padding: 0, marginTop: 8 }}
                  >
                    Görüntüle →
                  </Button>
                )}
              </Card>
            </Col>
          </Row>

          <Card title="Watchlist">
            {watchlist.length === 0 ? (
              <Text type="secondary">No symbols in watchlist</Text>
            ) : (
              <Row gutter={[16, 16]}>
                {watchlist.map(symbol => {
                  const tick = ticks.get(symbol);
                  const changeColor = tick && tick.change >= 0 ? '#3f8600' : '#cf1322';

                  return (
                    <Col span={6} key={symbol}>
                      <Card size="small">
                        <Space direction="vertical" size={0} style={{ width: '100%' }}>
                          <Text strong>{symbol}</Text>
                          <Title level={4} style={{ margin: 0 }}>
                            {tick ? `₺${tick.lastPrice.toFixed(2)}` : '--'}
                          </Title>
                          <Space>
                            <Text style={{ color: changeColor }}>
                              {tick ? (tick.change >= 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />) : null}
                              {tick ? `${tick.change.toFixed(2)}` : '--'}
                            </Text>
                            <Text style={{ color: changeColor }}>
                              {tick ? `${tick.changePercent.toFixed(2)}%` : '--'}
                            </Text>
                          </Space>
                        </Space>
                      </Card>
                    </Col>
                  );
                })}
              </Row>
            )}
          </Card>

          <Card title="Market Status">
            <Text>
              {isConnected
                ? 'WebSocket connected. Real-time market data streaming...'
                : 'WebSocket disconnected. Attempting to reconnect...'}
            </Text>
          </Card>
        </Space>
      </Content>
    </Layout>
  );
};

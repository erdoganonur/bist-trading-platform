import { useState } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Typography,
  Empty,
  Tooltip,
  Modal,
  Alert,
} from 'antd';
import {
  ReloadOutlined,
  EditOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { usePendingOrders } from '../hooks/usePendingOrders';
import { OrderModifyModal } from '../components/OrderModifyModal';
import { useAlgoLabStore } from '@app/store';
import type { AlgoLabPendingOrder } from '@services/api/broker.service';

const { Title, Text } = Typography;
const { confirm } = Modal;

/**
 * Pending Orders Page
 * Displays and manages AlgoLab pending orders (similar to Telegram Bot /pending command)
 */
export const PendingOrdersPage = () => {
  const { isAuthenticated } = useAlgoLabStore();
  const { orders, loading, fetchOrders, cancelOrder, modifyOrder } = usePendingOrders(
    isAuthenticated,
    30000 // Auto-refresh every 30 seconds
  );

  const [selectedOrder, setSelectedOrder] = useState<AlgoLabPendingOrder | null>(null);
  const [modifyModalOpen, setModifyModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  /**
   * Handle order cancellation with confirmation
   */
  const handleCancelOrder = (order: AlgoLabPendingOrder) => {
    confirm({
      title: 'Emri İptal Et',
      icon: <ExclamationCircleOutlined />,
      content: (
        <Space direction="vertical">
          <Text>Bu emri iptal etmek istediğinizden emin misiniz?</Text>
          <Alert
            message={
              <Space direction="vertical" size={0}>
                <Text>
                  <strong>Sembol:</strong> {order.ticker}
                </Text>
                <Text>
                  <strong>Yön:</strong> {order.buysell}
                </Text>
                <Text>
                  <strong>Fiyat:</strong>{' '}
                  {parseFloat(order.waitingprice || order.price) > 0
                    ? `₺${parseFloat(order.waitingprice || order.price).toFixed(2)}`
                    : 'Piyasa'}
                </Text>
                <Text>
                  <strong>Miktar:</strong> {order.ordersize} lot
                </Text>
              </Space>
            }
            type="warning"
            showIcon
          />
        </Space>
      ),
      okText: 'Evet, İptal Et',
      okType: 'danger',
      cancelText: 'Vazgeç',
      onOk: async () => {
        setActionLoading(true);
        try {
          await cancelOrder(order.atpref);
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  /**
   * Handle order modification
   */
  const handleModifyOrder = (order: AlgoLabPendingOrder) => {
    setSelectedOrder(order);
    setModifyModalOpen(true);
  };

  const handleModifySubmit = async (
    orderId: string,
    updates: { price?: number; lot?: number }
  ) => {
    setActionLoading(true);
    try {
      return await modifyOrder(orderId, updates);
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Table columns definition
   */
  const columns: ColumnsType<AlgoLabPendingOrder> = [
    {
      title: 'Sembol',
      dataIndex: 'ticker',
      key: 'ticker',
      width: 100,
      render: (ticker: string) => <Text strong>{ticker}</Text>,
    },
    {
      title: 'Yön',
      dataIndex: 'buysell',
      key: 'buysell',
      width: 80,
      align: 'center',
      render: (buysell: string) => (
        <Tag color={buysell === 'Alış' ? 'green' : 'red'}>
          {buysell === 'Alış' ? 'ALIŞ' : 'SATIŞ'}
        </Tag>
      ),
    },
    {
      title: 'Tip',
      key: 'type',
      width: 90,
      align: 'center',
      render: (_, record) => {
        const isLimit = parseFloat(record.waitingprice || record.price) > 0;
        return (
          <Tag color={isLimit ? 'blue' : 'orange'}>
            {isLimit ? 'Limitli' : 'Piyasa'}
          </Tag>
        );
      },
    },
    {
      title: 'Fiyat',
      key: 'price',
      width: 120,
      align: 'right',
      render: (_, record) => {
        const displayPrice = parseFloat(record.waitingprice || record.price);
        return (
          <Text style={{ fontFamily: 'monospace' }}>
            {displayPrice > 0 ? `₺${displayPrice.toFixed(2)}` : 'Piyasa'}
          </Text>
        );
      },
    },
    {
      title: 'Miktar (Lot)',
      dataIndex: 'ordersize',
      key: 'ordersize',
      width: 120,
      align: 'right',
      render: (ordersize: string) => (
        <Text style={{ fontFamily: 'monospace' }}>{ordersize}</Text>
      ),
    },
    {
      title: 'Durum',
      dataIndex: 'description',
      key: 'description',
      width: 150,
      render: (description: string) => (
        <Tag icon={<ClockCircleOutlined />} color="processing">
          {description || 'BEKLEMEDE'}
        </Tag>
      ),
    },
    {
      title: 'Tarih/Saat',
      key: 'datetime',
      width: 150,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: '12px' }}>{record.transactiontime}</Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {record.timetransaction}
          </Text>
        </Space>
      ),
    },
    {
      title: 'İşlemler',
      key: 'actions',
      width: 120,
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Düzenle">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => handleModifyOrder(record)}
              disabled={loading || actionLoading}
            />
          </Tooltip>
          <Tooltip title="İptal Et">
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => handleCancelOrder(record)}
              disabled={loading || actionLoading}
            />
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '24px' }}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {/* Page Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={2} style={{ margin: 0 }}>
              Bekleyen Emirler
            </Title>
            <Text type="secondary">
              AlgoLab'da bekleyen emirleriniz (Her 30 saniyede otomatik yenilenir)
            </Text>
          </div>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchOrders()}
            loading={loading}
          >
            Yenile
          </Button>
        </div>

        {/* Authentication Warning */}
        {!isAuthenticated && (
          <Alert
            message="AlgoLab Girişi Gerekli"
            description="Bekleyen emirleri görüntülemek için AlgoLab broker hesabınızla giriş yapmalısınız."
            type="warning"
            showIcon
            closable
          />
        )}

        {/* Orders Table */}
        <Card>
          <Table
            dataSource={orders}
            columns={columns}
            rowKey="atpref"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Toplam ${total} emir`,
            }}
            locale={{
              emptyText: (
                <Empty
                  description={
                    isAuthenticated
                      ? 'Bekleyen emir bulunmamaktadır'
                      : 'AlgoLab girişi yapınız'
                  }
                />
              ),
            }}
            scroll={{ x: 1000 }}
          />
        </Card>

        {/* Info Alert */}
        {isAuthenticated && orders.length > 0 && (
          <Alert
            message="Bilgilendirme"
            description={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>Emirler her 30 saniyede otomatik olarak yenilenir</li>
                <li>Emir düzenlemek için kalem ikonuna tıklayın</li>
                <li>Emir iptal etmek için çöp kutusu ikonuna tıklayın</li>
                <li>
                  Limitli emirlerde hem fiyat hem miktar değiştirilebilir
                </li>
              </ul>
            }
            type="info"
            showIcon
          />
        )}
      </Space>

      {/* Order Modify Modal */}
      <OrderModifyModal
        open={modifyModalOpen}
        order={selectedOrder}
        loading={actionLoading}
        onClose={() => {
          setModifyModalOpen(false);
          setSelectedOrder(null);
        }}
        onSubmit={handleModifySubmit}
      />
    </div>
  );
};

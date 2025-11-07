import React, { useState } from 'react';
import { Table, Tag, Button, Space, Empty, Modal, message, InputNumber, Form } from 'antd';
import {
  EditOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { Widget, OrderSideBadge, TradingButton } from '@components/ui';
import { formatCurrency, formatNumber } from '@utils/formatters';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { brokerApi } from '@services/api';
import dayjs from 'dayjs';

interface PendingOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  price: number;
  quantity: number;
  status: string;
  createdAt: string;
}

export const PendingOrdersWidget: React.FC = () => {
  const [modifyModalOpen, setModifyModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<PendingOrder | null>(null);
  const [form] = Form.useForm();
  const queryClient = useQueryClient();

  const { data: orders, isLoading, refetch } = useQuery({
    queryKey: ['pendingOrders'],
    queryFn: brokerApi.getPendingOrders,
    refetchInterval: 3000, // Refresh every 3 seconds
  });

  // Cancel order mutation
  const cancelMutation = useMutation({
    mutationFn: (orderId: string) => brokerApi.cancelOrder(orderId),
    onSuccess: () => {
      message.success('Order cancelled successfully');
      queryClient.invalidateQueries({ queryKey: ['pendingOrders'] });
    },
    onError: (error: any) => {
      message.error(error.message || 'Failed to cancel order');
    },
  });

  // Modify order mutation
  const modifyMutation = useMutation({
    mutationFn: ({ orderId, price, lot }: { orderId: string; price: number; lot: number }) =>
      brokerApi.modifyOrder(orderId, { price, lot }),
    onSuccess: () => {
      message.success('Order modified successfully');
      setModifyModalOpen(false);
      setSelectedOrder(null);
      form.resetFields();
      queryClient.invalidateQueries({ queryKey: ['pendingOrders'] });
    },
    onError: (error: any) => {
      message.error(error.message || 'Failed to modify order');
    },
  });

  const handleCancel = (order: PendingOrder) => {
    Modal.confirm({
      title: 'Cancel Order',
      icon: <ExclamationCircleOutlined />,
      content: `Are you sure you want to cancel this ${order.side} order for ${order.symbol}?`,
      okText: 'Yes, Cancel',
      okType: 'danger',
      onOk: () => cancelMutation.mutate(order.id),
    });
  };

  const handleModify = (order: PendingOrder) => {
    setSelectedOrder(order);
    form.setFieldsValue({
      price: order.price,
      lot: order.quantity / 100, // Convert to lots
    });
    setModifyModalOpen(true);
  };

  const handleModifySubmit = (values: { price: number; lot: number }) => {
    if (!selectedOrder) return;
    modifyMutation.mutate({
      orderId: selectedOrder.id,
      price: values.price,
      lot: values.lot * 100, // Convert to shares
    });
  };

  const columns: any = [
    {
      title: 'Time',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 100,
      render: (date: string) => (
        <span className="text-xs text-gray-500">
          {dayjs(date).format('HH:mm:ss')}
        </span>
      ),
    },
    {
      title: 'Symbol',
      dataIndex: 'symbol',
      key: 'symbol',
      width: 100,
      render: (symbol: string) => (
        <span className="font-semibold text-primary-600">{symbol}</span>
      ),
    },
    {
      title: 'Side',
      dataIndex: 'side',
      key: 'side',
      width: 100,
      render: (side: 'BUY' | 'SELL') => <OrderSideBadge side={side} />,
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      width: 80,
      render: (type: string) => (
        <Tag color={type === 'LIMIT' ? 'blue' : 'orange'}>{type}</Tag>
      ),
    },
    {
      title: 'Price',
      dataIndex: 'price',
      key: 'price',
      width: 100,
      align: 'right',
      render: (price: number, record) =>
        record.type === 'LIMIT' ? (
          <span className="font-mono">{formatCurrency(price)}</span>
        ) : (
          <span className="text-gray-400">Market</span>
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
      title: 'Value',
      key: 'value',
      width: 120,
      align: 'right',
      render: (_, record) => {
        const value = record.type === 'LIMIT' ? record.price * record.quantity : 0;
        return value > 0 ? (
          <span className="font-mono">{formatCurrency(value)}</span>
        ) : (
          <span className="text-gray-400">-</span>
        );
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color="processing" icon={<ClockCircleOutlined />}>
          {status}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right',
      width: 120,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleModify(record)}
            disabled={record.type === 'MARKET'}
            title="Modify Order"
          />
          <Button
            type="text"
            size="small"
            icon={<DeleteOutlined />}
            danger
            onClick={() => handleCancel(record)}
            loading={cancelMutation.isPending}
            title="Cancel Order"
          />
        </Space>
      ),
    },
  ];

  return (
    <>
      <Widget
        title="Pending Orders"
        icon={<ClockCircleOutlined />}
        onRefresh={refetch}
        extra={
          <Tag color="orange">
            {orders?.length || 0} Order{orders?.length !== 1 ? 's' : ''}
          </Tag>
        }
      >
        <Table
          columns={columns}
          dataSource={orders}
          loading={isLoading}
          rowKey="id"
          size="small"
          scroll={{ x: 800 }}
          pagination={false}
          locale={{
            emptyText: (
              <Empty
                description="No pending orders"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              />
            ),
          }}
        />
      </Widget>

      {/* Modify Order Modal */}
      <Modal
        title={`Modify Order - ${selectedOrder?.symbol}`}
        open={modifyModalOpen}
        onCancel={() => {
          setModifyModalOpen(false);
          setSelectedOrder(null);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleModifySubmit}
        >
          <Form.Item
            label="Price (₺)"
            name="price"
            rules={[{ required: true, message: 'Please enter price' }]}
          >
            <InputNumber
              size="large"
              className="w-full"
              min={0}
              step={0.01}
              precision={2}
            />
          </Form.Item>

          <Form.Item
            label="Quantity (Lot)"
            name="lot"
            rules={[{ required: true, message: 'Please enter quantity' }]}
            tooltip="1 lot = 100 shares"
          >
            <InputNumber
              size="large"
              className="w-full"
              min={1}
              step={1}
              addonAfter="lot"
            />
          </Form.Item>

          <Form.Item>
            <Space className="w-full justify-end">
              <Button onClick={() => setModifyModalOpen(false)}>
                Cancel
              </Button>
              <TradingButton
                action="modify"
                htmlType="submit"
                loading={modifyMutation.isPending}
              >
                Modify Order
              </TradingButton>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default PendingOrdersWidget;

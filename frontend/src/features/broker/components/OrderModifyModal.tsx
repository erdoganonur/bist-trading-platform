import { Modal, Form, InputNumber, Button, Space, Typography, Alert, Divider } from 'antd';
import { EditOutlined } from '@ant-design/icons';
import { useState, useEffect } from 'react';
import type { AlgoLabPendingOrder } from '@services/api/broker.service';
import { calculateOrderCost, formatCostEstimate } from '@services/api/broker.service';

const { Text, Paragraph } = Typography;

interface OrderModifyModalProps {
  open: boolean;
  order: AlgoLabPendingOrder | null;
  loading?: boolean;
  onClose: () => void;
  onSubmit: (orderId: string, updates: { price?: number; lot?: number }) => Promise<boolean>;
}

/**
 * Order Modification Modal
 * Allows users to modify price and/or quantity of pending orders
 */
export const OrderModifyModal = ({
  open,
  order,
  loading = false,
  onClose,
  onSubmit,
}: OrderModifyModalProps) => {
  const [form] = Form.useForm();
  const [costEstimate, setCostEstimate] = useState<string>('');

  // Reset form when order changes
  useEffect(() => {
    if (order && open) {
      form.setFieldsValue({
        price: parseFloat(order.price),
        lot: parseInt(order.lot),
      });
      updateCostEstimate(parseFloat(order.price), parseInt(order.lot));
    }
  }, [order, open, form]);

  const updateCostEstimate = (price: number, lot: number) => {
    if (!order || !price || !lot) {
      setCostEstimate('');
      return;
    }

    const isBuy = order.direction === '0'; // "0" = BUY
    const estimate = calculateOrderCost(lot, price, isBuy);
    setCostEstimate(formatCostEstimate(estimate));
  };

  const handleValuesChange = (_: any, allValues: any) => {
    const { price, lot } = allValues;
    if (price && lot) {
      updateCostEstimate(price, lot);
    }
  };

  const handleSubmit = async (values: { price?: number; lot?: number }) => {
    if (!order) return;

    const updates: { price?: number; lot?: number } = {};

    // Only include changed values
    if (values.price !== parseFloat(order.price)) {
      updates.price = values.price;
    }
    if (values.lot !== parseInt(order.lot)) {
      updates.lot = values.lot;
    }

    if (Object.keys(updates).length === 0) {
      Modal.info({
        title: 'Değişiklik Yok',
        content: 'Herhangi bir değişiklik yapmadınız.',
      });
      return;
    }

    const success = await onSubmit(order.atpref, updates);
    if (success) {
      handleClose();
    }
  };

  const handleClose = () => {
    form.resetFields();
    setCostEstimate('');
    onClose();
  };

  if (!order) return null;

  const isBuy = order.direction === '0';
  const isLimit = order.priceType === 'L';

  return (
    <Modal
      title={
        <Space>
          <EditOutlined />
          <span>Emir Düzenle</span>
        </Space>
      }
      open={open}
      onCancel={handleClose}
      footer={null}
      width={500}
      destroyOnClose
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        {/* Order Info */}
        <Alert
          message="Mevcut Emir Bilgileri"
          description={
            <Space direction="vertical" size={0}>
              <Text>
                <strong>Sembol:</strong> {order.symbol}
              </Text>
              <Text>
                <strong>Yön:</strong>{' '}
                <Text type={isBuy ? 'success' : 'danger'}>
                  {isBuy ? 'ALIŞ' : 'SATIŞ'}
                </Text>
              </Text>
              <Text>
                <strong>Tip:</strong> {isLimit ? 'Limitli' : 'Piyasa'}
              </Text>
              <Text>
                <strong>Fiyat:</strong> ₺{parseFloat(order.price).toFixed(2)}
              </Text>
              <Text>
                <strong>Miktar:</strong> {order.lot} lot
              </Text>
              <Text>
                <strong>Durum:</strong> {order.status}
              </Text>
            </Space>
          }
          type="info"
          showIcon
        />

        <Divider style={{ margin: '8px 0' }} />

        {/* Modification Form */}
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          onValuesChange={handleValuesChange}
          autoComplete="off"
        >
          {isLimit && (
            <Form.Item
              name="price"
              label="Yeni Fiyat (₺)"
              rules={[
                { required: true, message: 'Fiyat giriniz' },
                { type: 'number', min: 0.01, message: 'Geçerli bir fiyat giriniz' },
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                size="large"
                precision={2}
                min={0.01}
                step={0.01}
                placeholder="Yeni fiyat"
              />
            </Form.Item>
          )}

          <Form.Item
            name="lot"
            label="Yeni Miktar (Lot)"
            rules={[
              { required: true, message: 'Miktar giriniz' },
              { type: 'number', min: 1, message: 'En az 1 lot giriniz' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              size="large"
              min={1}
              step={1}
              placeholder="Yeni miktar"
            />
          </Form.Item>

          {/* Cost Estimate */}
          {costEstimate && (
            <Alert
              message="Tahmini Maliyet"
              description={
                <pre style={{ margin: 0, fontFamily: 'monospace', fontSize: '12px' }}>
                  {costEstimate}
                </pre>
              }
              type="warning"
              showIcon
            />
          )}

          <Form.Item style={{ marginTop: 16, marginBottom: 0 }}>
            <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
              <Button onClick={handleClose} disabled={loading}>
                İptal
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                Emri Güncelle
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Alert
          message="Uyarı"
          description="Emir güncellendiğinde AlgoLab'da yeni bir emir olarak işleme alınır. Eski emir iptal edilir ve yerine yeni emir oluşturulur."
          type="warning"
          showIcon
        />
      </Space>
    </Modal>
  );
};

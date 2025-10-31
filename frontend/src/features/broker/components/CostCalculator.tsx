import { Card, Form, InputNumber, Radio, Space, Typography, Divider, Alert } from 'antd';
import { CalculatorOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { calculateOrderCost } from '@services/api/broker.service';
import type { OrderCostEstimate } from '@services/api/broker.service';

const { Title, Text } = Typography;

/**
 * Cost Calculator Component
 * Calculates Turkish market fees (commission, BSMV, BIST fee)
 * Similar to Telegram Bot's cost calculator
 */
export const CostCalculator = () => {
  const [form] = Form.useForm();
  const [estimate, setEstimate] = useState<OrderCostEstimate | null>(null);

  const handleCalculate = (values: any) => {
    const { quantity, price, direction } = values;

    if (quantity && price) {
      const isBuy = direction === 'buy';
      const result = calculateOrderCost(quantity, price, isBuy);
      setEstimate(result);
    }
  };

  const handleValuesChange = (_: any, allValues: any) => {
    handleCalculate(allValues);
  };

  return (
    <Card>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <div>
          <Title level={4} style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CalculatorOutlined />
            <span>Maliyet Hesaplayıcı</span>
          </Title>
          <Text type="secondary">
            Türk borsası ücretlerini hesaplayın (Komisyon, BSMV, BIST ücreti)
          </Text>
        </div>

        <Form
          form={form}
          layout="vertical"
          onValuesChange={handleValuesChange}
          initialValues={{ direction: 'buy' }}
        >
          <Form.Item
            name="direction"
            label="Yön"
            rules={[{ required: true, message: 'Yön seçiniz' }]}
          >
            <Radio.Group buttonStyle="solid">
              <Radio.Button value="buy">ALIŞ</Radio.Button>
              <Radio.Button value="sell">SATIŞ</Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            name="price"
            label="Fiyat (₺)"
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
              placeholder="Hisse fiyatı"
            />
          </Form.Item>

          <Form.Item
            name="quantity"
            label="Miktar (Adet)"
            rules={[
              { required: true, message: 'Miktar giriniz' },
              { type: 'number', min: 1, message: 'En az 1 adet giriniz' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              size="large"
              min={1}
              step={1}
              placeholder="Hisse adedi"
            />
          </Form.Item>
        </Form>

        {estimate && (
          <>
            <Divider />
            <Alert
              message="Maliyet Özeti"
              description={
                <Space direction="vertical" style={{ width: '100%' }} size="small">
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text>Toplam Değer:</Text>
                    <Text strong style={{ fontFamily: 'monospace' }}>
                      ₺{estimate.baseValue.toFixed(2)}
                    </Text>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text type="secondary">Komisyon (0.2%):</Text>
                    <Text type="secondary" style={{ fontFamily: 'monospace' }}>
                      ₺{estimate.commission.toFixed(2)}
                    </Text>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text type="secondary">BSMV (0.1%):</Text>
                    <Text type="secondary" style={{ fontFamily: 'monospace' }}>
                      ₺{estimate.bsmv.toFixed(2)}
                    </Text>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text type="secondary">BIST Ücreti (0.003%):</Text>
                    <Text type="secondary" style={{ fontFamily: 'monospace' }}>
                      ₺{estimate.bistFee.toFixed(2)}
                    </Text>
                  </div>
                  <Divider style={{ margin: '8px 0' }} />
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text type="secondary">Toplam Ücretler:</Text>
                    <Text type="secondary" style={{ fontFamily: 'monospace' }}>
                      ₺{estimate.totalFees.toFixed(2)}
                    </Text>
                  </div>
                  <Divider style={{ margin: '8px 0' }} />
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Text strong style={{ fontSize: '16px' }}>
                      {estimate.isBuy ? 'Ödenecek Tutar:' : 'Alınacak Tutar:'}
                    </Text>
                    <Text
                      strong
                      style={{
                        fontSize: '16px',
                        fontFamily: 'monospace',
                        color: estimate.isBuy ? '#ff4d4f' : '#52c41a',
                      }}
                    >
                      ₺{estimate.grandTotal.toFixed(2)}
                    </Text>
                  </div>
                </Space>
              }
              type={estimate.isBuy ? 'warning' : 'success'}
              showIcon
            />

            <Alert
              message="Bilgilendirme"
              description={
                <ul style={{ margin: 0, paddingLeft: 20 }}>
                  <li>Komisyon oranı: %0.2 (broker'a göre değişebilir)</li>
                  <li>BSMV (Banka ve Sigorta Muameleleri Vergisi): Komisyon üzerinden %0.1</li>
                  <li>BIST İşlem Ücreti: %0.003</li>
                  <li>Alış işlemlerinde ücretler eklenir, satışta düşülür</li>
                </ul>
              }
              type="info"
              showIcon
            />
          </>
        )}
      </Space>
    </Card>
  );
};

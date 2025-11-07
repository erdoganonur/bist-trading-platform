import React, { useState, useEffect } from 'react';
import {
  Form,
  InputNumber,
  Radio,
  Divider,
  Alert,
  AutoComplete,
  message,
} from 'antd';
import { SearchOutlined, DollarOutlined } from '@ant-design/icons';
import { Widget, TradingButton } from '@components/ui';
import { calculateOrderCost, formatCurrency } from '@utils/formatters';
import { useMutation, useQuery } from '@tanstack/react-query';
import { brokerApi, symbolApi } from '@services/api';

interface QuickTradeForm {
  symbol: string;
  side: 'BUY' | 'SELL';
  priceType: 'LIMIT' | 'MARKET';
  price: number;
  lot: number;
}

export const QuickTradeWidget: React.FC = () => {
  const [form] = Form.useForm<QuickTradeForm>();
  const [searchValue, setSearchValue] = useState('');
  const [cost, setCost] = useState<ReturnType<typeof calculateOrderCost> | null>(null);

  const side = Form.useWatch('side', form) || 'BUY';
  const priceType = Form.useWatch('priceType', form) || 'LIMIT';
  const price = Form.useWatch('price', form) || 0;
  const lot = Form.useWatch('lot', form) || 0;

  // Search symbols
  const { data: symbols } = useQuery({
    queryKey: ['symbols', searchValue],
    queryFn: () => symbolApi.searchSymbols(searchValue),
    enabled: searchValue.length >= 2,
  });

  // Place order mutation
  const placeOrderMutation = useMutation({
    mutationFn: brokerApi.placeOrder,
    onSuccess: () => {
      message.success('Order placed successfully!');
      form.resetFields();
      setCost(null);
    },
    onError: (error: any) => {
      message.error(error.message || 'Failed to place order');
    },
  });

  // Calculate cost when values change
  useEffect(() => {
    if (price > 0 && lot > 0) {
      const quantity = lot * 100; // 1 lot = 100 shares
      const calculatedCost = calculateOrderCost(price, quantity, side);
      setCost(calculatedCost);
    } else {
      setCost(null);
    }
  }, [price, lot, side]);

  const handleSubmit = (values: QuickTradeForm) => {
    const quantity = values.lot * 100;
    placeOrderMutation.mutate({
      symbol: values.symbol,
      direction: values.side === 'BUY' ? '0' : '1',
      priceType: values.priceType === 'LIMIT' ? 'L' : 'P',
      price: values.priceType === 'LIMIT' ? values.price : undefined,
      lot: quantity,
      smsNotification: 'H',
      emailNotification: 'H',
    });
  };

  const symbolOptions = symbols?.map((symbol: any) => ({
    value: symbol.symbol,
    label: (
      <div className="flex justify-between">
        <span className="font-semibold">{symbol.symbol}</span>
        <span className="text-gray-500">{symbol.name}</span>
      </div>
    ),
  })) || [];

  return (
    <Widget
      title="Quick Trade"
      icon={<DollarOutlined />}
      className="h-full"
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          side: 'BUY',
          priceType: 'LIMIT',
          lot: 1,
        }}
      >
        {/* Symbol Search */}
        <Form.Item
          label="Symbol"
          name="symbol"
          rules={[{ required: true, message: 'Please select a symbol' }]}
        >
          <AutoComplete
            options={symbolOptions}
            onSearch={setSearchValue}
            placeholder="Search symbol (e.g., AKBNK)"
            size="large"
            suffixIcon={<SearchOutlined />}
            filterOption={false}
          />
        </Form.Item>

        {/* Buy/Sell Toggle */}
        <Form.Item name="side" label="Side">
          <Radio.Group
            buttonStyle="solid"
            size="large"
            className="w-full grid grid-cols-2 gap-2"
          >
            <Radio.Button value="BUY" className="text-center">
              <span className="text-trading-profit font-semibold">BUY</span>
            </Radio.Button>
            <Radio.Button value="SELL" className="text-center">
              <span className="text-trading-loss font-semibold">SELL</span>
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        {/* Price Type */}
        <Form.Item name="priceType" label="Order Type">
          <Radio.Group buttonStyle="solid" size="middle" className="w-full">
            <Radio.Button value="LIMIT" className="flex-1 text-center">
              Limit
            </Radio.Button>
            <Radio.Button value="MARKET" className="flex-1 text-center">
              Market
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        {/* Price Input (only for LIMIT) */}
        {priceType === 'LIMIT' && (
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
              placeholder="0.00"
            />
          </Form.Item>
        )}

        {/* Quantity (Lot) */}
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
            placeholder="1"
            addonAfter="lot"
          />
        </Form.Item>

        {/* Cost Calculation */}
        {cost && (
          <div className="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg mb-4">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Base Value:</span>
                <span className="font-mono">{formatCurrency(cost.baseValue)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Commission (0.2%):</span>
                <span className="font-mono">{formatCurrency(cost.commission)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">BSMV Tax:</span>
                <span className="font-mono">{formatCurrency(cost.bsmv)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">BIST Fee:</span>
                <span className="font-mono">{formatCurrency(cost.bistFee)}</span>
              </div>
              <Divider style={{ margin: '8px 0' }} />
              <div className="flex justify-between text-base font-semibold">
                <span>Total {side === 'BUY' ? 'Cost' : 'Proceeds'}:</span>
                <span className={`font-mono ${side === 'BUY' ? 'text-trading-loss' : 'text-trading-profit'}`}>
                  {formatCurrency(cost.total)}
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Submit Button */}
        <Form.Item>
          <TradingButton
            action={side.toLowerCase() as 'buy' | 'sell'}
            htmlType="submit"
            size="large"
            block
            loading={placeOrderMutation.isPending}
          >
            {side === 'BUY' ? 'Place Buy Order' : 'Place Sell Order'}
          </TradingButton>
        </Form.Item>

        {placeOrderMutation.isError && (
          <Alert
            type="error"
            message="Order Failed"
            description={placeOrderMutation.error?.message}
            showIcon
            closable
          />
        )}
      </Form>
    </Widget>
  );
};

export default QuickTradeWidget;

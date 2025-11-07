import React from 'react';
import { Tag } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';

export interface OrderSideBadgeProps {
  side: 'BUY' | 'SELL' | '0' | '1';
  showIcon?: boolean;
  size?: 'small' | 'default' | 'large';
}

export const OrderSideBadge: React.FC<OrderSideBadgeProps> = ({
  side,
  showIcon = true,
  size = 'default',
}) => {
  const isBuy = side === 'BUY' || side === '0';
  const label = isBuy ? 'BUY' : 'SELL';

  return (
    <Tag
      color={isBuy ? 'success' : 'error'}
      icon={showIcon ? (isBuy ? <ArrowUpOutlined /> : <ArrowDownOutlined />) : undefined}
      className={`
        font-semibold
        ${size === 'small' ? 'text-xs px-2 py-0' : ''}
        ${size === 'large' ? 'text-base px-4 py-1' : ''}
      `}
    >
      {label}
    </Tag>
  );
};

export default OrderSideBadge;

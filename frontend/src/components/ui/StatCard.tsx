import React, { type ReactNode } from 'react';
import { Card, Statistic, Space } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import { motion } from 'framer-motion';

export interface StatCardProps {
  title: string;
  value: string | number;
  prefix?: string;
  suffix?: string;
  icon?: ReactNode;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: string | number;
  loading?: boolean;
  className?: string;
  valueStyle?: React.CSSProperties;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  prefix,
  suffix,
  icon,
  trend,
  trendValue,
  loading = false,
  className = '',
  valueStyle,
}) => {
  const getTrendColor = () => {
    if (trend === 'up') return 'text-trading-profit';
    if (trend === 'down') return 'text-trading-loss';
    return 'text-gray-500';
  };

  const getTrendIcon = () => {
    if (trend === 'up') return <ArrowUpOutlined />;
    if (trend === 'down') return <ArrowDownOutlined />;
    return null;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <Card
        loading={loading}
        className={`shadow-widget hover:shadow-widget-hover transition-all ${className}`}
        bordered={false}
      >
        <Space direction="vertical" size={4} className="w-full">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-500">{title}</span>
            {icon && <span className="text-lg opacity-60">{icon}</span>}
          </div>

          <Statistic
            value={value}
            prefix={prefix}
            suffix={suffix}
            valueStyle={{
              fontSize: '24px',
              fontWeight: 600,
              ...valueStyle,
            }}
          />

          {trendValue && (
            <div className={`flex items-center gap-1 text-sm ${getTrendColor()}`}>
              {getTrendIcon()}
              <span>{trendValue}</span>
            </div>
          )}
        </Space>
      </Card>
    </motion.div>
  );
};

export default StatCard;

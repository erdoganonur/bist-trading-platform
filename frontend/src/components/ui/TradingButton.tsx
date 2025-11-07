import React from 'react';
import { Button, type ButtonProps } from 'antd';

export interface TradingButtonProps extends Omit<ButtonProps, 'type'> {
  action: 'buy' | 'sell' | 'cancel' | 'modify' | 'close';
}

export const TradingButton: React.FC<TradingButtonProps> = ({
  action,
  children,
  className = '',
  ...props
}) => {
  const getButtonStyles = () => {
    switch (action) {
      case 'buy':
        return 'bg-trading-profit hover:bg-trading-profit-dark text-white border-trading-profit';
      case 'sell':
        return 'bg-trading-loss hover:bg-trading-loss-dark text-white border-trading-loss';
      case 'cancel':
        return 'border-red-500 text-red-500 hover:bg-red-50';
      case 'modify':
        return 'border-blue-500 text-blue-500 hover:bg-blue-50';
      case 'close':
        return 'border-orange-500 text-orange-500 hover:bg-orange-50';
      default:
        return '';
    }
  };

  return (
    <Button
      className={`font-medium transition-all ${getButtonStyles()} ${className}`}
      {...props}
    >
      {children}
    </Button>
  );
};

export default TradingButton;

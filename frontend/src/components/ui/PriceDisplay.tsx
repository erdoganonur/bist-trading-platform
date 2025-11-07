import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

export interface PriceDisplayProps {
  value: number;
  previousValue?: number;
  prefix?: string;
  suffix?: string;
  decimals?: number;
  className?: string;
  animated?: boolean;
}

export const PriceDisplay: React.FC<PriceDisplayProps> = ({
  value,
  previousValue,
  prefix = '',
  suffix = '',
  decimals = 2,
  className = '',
  animated = true,
}) => {
  const [flashClass, setFlashClass] = useState('');

  useEffect(() => {
    if (previousValue !== undefined && previousValue !== value && animated) {
      const direction = value > previousValue ? 'up' : 'down';
      setFlashClass(direction === 'up' ? 'price-flash-up' : 'price-flash-down');

      const timer = setTimeout(() => setFlashClass(''), 600);
      return () => clearTimeout(timer);
    }
  }, [value, previousValue, animated]);

  const getTextColor = () => {
    if (previousValue === undefined) return '';
    if (value > previousValue) return 'text-trading-profit';
    if (value < previousValue) return 'text-trading-loss';
    return '';
  };

  const formattedValue = value.toFixed(decimals);

  return (
    <AnimatePresence mode="wait">
      <motion.span
        key={value}
        initial={animated ? { scale: 1.1 } : {}}
        animate={{ scale: 1 }}
        transition={{ duration: 0.2 }}
        className={`${flashClass} ${getTextColor()} ${className} font-mono inline-block`}
      >
        {prefix}{formattedValue}{suffix}
      </motion.span>
    </AnimatePresence>
  );
};

export default PriceDisplay;

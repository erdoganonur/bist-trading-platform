import React, { type ReactNode } from 'react';
import { CCard, CCardBody, CSpinner } from '@coreui/react';
import { cilArrowTop, cilArrowBottom } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
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
    if (trend === 'up') return 'text-success';
    if (trend === 'down') return 'text-danger';
    return 'text-secondary';
  };

  const getTrendIcon = () => {
    if (trend === 'up') return <CIcon icon={cilArrowTop} size="sm" />;
    if (trend === 'down') return <CIcon icon={cilArrowBottom} size="sm" />;
    return null;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <CCard className={`shadow-sm border-0 ${className}`}>
        <CCardBody>
          {loading ? (
            <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '100px' }}>
              <CSpinner color="primary" size="sm" />
            </div>
          ) : (
            <div className="d-flex flex-column gap-2">
              <div className="d-flex align-items-center justify-content-between">
                <span className="small text-secondary">{title}</span>
                {icon && <span className="fs-5 opacity-75">{icon}</span>}
              </div>

              <div
                className="fs-4 fw-semibold"
                style={valueStyle}
              >
                {prefix}
                {value}
                {suffix}
              </div>

              {trendValue && (
                <div className={`d-flex align-items-center gap-1 small ${getTrendColor()}`}>
                  {getTrendIcon()}
                  <span>{trendValue}</span>
                </div>
              )}
            </div>
          )}
        </CCardBody>
      </CCard>
    </motion.div>
  );
};

export default StatCard;

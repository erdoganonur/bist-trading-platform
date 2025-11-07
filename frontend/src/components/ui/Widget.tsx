import React, { type ReactNode } from 'react';
import { Card, Space, Button, Dropdown } from 'antd';
import { MoreOutlined, ExpandOutlined, SyncOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';

export interface WidgetProps {
  title?: string;
  icon?: ReactNode;
  extra?: ReactNode;
  children: ReactNode;
  loading?: boolean;
  className?: string;
  onRefresh?: () => void;
  onExpand?: () => void;
  menuItems?: MenuProps['items'];
  bodyClassName?: string;
  headerClassName?: string;
}

export const Widget: React.FC<WidgetProps> = ({
  title,
  icon,
  extra,
  children,
  loading = false,
  className = '',
  onRefresh,
  onExpand,
  menuItems,
  bodyClassName = '',
  headerClassName = '',
}) => {
  const actionItems = [
    ...(menuItems || []),
    onRefresh && {
      key: 'refresh',
      label: 'Refresh',
      icon: <SyncOutlined />,
      onClick: onRefresh,
    },
    onExpand && {
      key: 'expand',
      label: 'Expand',
      icon: <ExpandOutlined />,
      onClick: onExpand,
    },
  ].filter(Boolean) as MenuProps['items'];

  const widgetExtra = (
    <Space size="small">
      {extra}
      {actionItems && actionItems.length > 0 && (
        <Dropdown menu={{ items: actionItems }} trigger={['click']}>
          <Button
            type="text"
            icon={<MoreOutlined />}
            size="small"
            className="opacity-60 hover:opacity-100"
          />
        </Dropdown>
      )}
    </Space>
  );

  return (
    <Card
      title={
        title && (
          <div className={`flex items-center gap-2 ${headerClassName}`}>
            {icon}
            <span className="font-medium text-sm">{title}</span>
          </div>
        )
      }
      extra={widgetExtra}
      loading={loading}
      className={`widget shadow-sm hover:shadow-md transition-shadow ${className}`}
      bodyStyle={{ padding: '16px' }}
      headStyle={{ borderBottom: '1px solid #f0f0f0', padding: '12px 16px' }}
    >
      <div className={bodyClassName}>{children}</div>
    </Card>
  );
};

export default Widget;

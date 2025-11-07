import React, { type ReactNode, useState } from 'react';
import { Layout, Button, Space, Avatar, Dropdown, Badge, theme } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useAuthStore } from '@app/store';
import { useNavigate } from 'react-router-dom';

const { Header, Sider, Content } = Layout;

export interface DashboardLayoutProps {
  children: ReactNode;
  sider?: ReactNode;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children, sider }) => {
  const [collapsed, setCollapsed] = useState(false);
  const { token } = theme.useToken();
  const { user, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      label: 'Profile',
      icon: <UserOutlined />,
      onClick: () => navigate('/profile'),
    },
    {
      key: 'settings',
      label: 'Settings',
      icon: <SettingOutlined />,
      onClick: () => navigate('/settings'),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      label: 'Logout',
      icon: <LogoutOutlined />,
      danger: true,
      onClick: handleLogout,
    },
  ];

  return (
    <Layout className="min-h-screen">
      {/* Header */}
      <Header
        style={{
          position: 'fixed',
          top: 0,
          zIndex: 1000,
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
          background: token.colorBgContainer,
          borderBottom: `1px solid ${token.colorBorderSecondary}`,
          height: 'var(--header-height)',
        }}
      >
        {/* Left Section */}
        <div className="flex items-center gap-4">
          {sider && (
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              style={{
                fontSize: '16px',
                width: 40,
                height: 40,
              }}
            />
          )}
          <div className="flex items-center gap-2">
            <div className="text-xl font-bold bg-gradient-to-r from-primary-500 to-secondary-500 bg-clip-text text-transparent">
              BIST Trading
            </div>
          </div>
        </div>

        {/* Right Section */}
        <Space size="middle">
          {/* Notifications */}
          <Badge count={0} showZero={false}>
            <Button
              type="text"
              icon={<BellOutlined style={{ fontSize: '18px' }} />}
              className="flex items-center justify-center"
            />
          </Badge>

          {/* User Menu */}
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight" trigger={['click']}>
            <div className="flex items-center gap-2 cursor-pointer hover:opacity-80 transition-opacity">
              <Avatar size="default" icon={<UserOutlined />} />
              <span className="font-medium">{user?.username || 'User'}</span>
            </div>
          </Dropdown>
        </Space>
      </Header>

      <Layout style={{ marginTop: 'var(--header-height)' }}>
        {/* Sidebar */}
        {sider && (
          <Sider
            trigger={null}
            collapsible
            collapsed={collapsed}
            width={240}
            collapsedWidth={80}
            style={{
              overflow: 'auto',
              height: 'calc(100vh - var(--header-height))',
              position: 'fixed',
              left: 0,
              top: 'var(--header-height)',
              bottom: 0,
              background: token.colorBgContainer,
              borderRight: `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            {sider}
          </Sider>
        )}

        {/* Main Content */}
        <Content
          style={{
            marginLeft: sider ? (collapsed ? 80 : 240) : 0,
            padding: '16px',
            minHeight: 'calc(100vh - var(--header-height))',
            background: token.colorBgLayout,
            transition: 'margin-left 0.2s',
          }}
        >
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};

export default DashboardLayout;

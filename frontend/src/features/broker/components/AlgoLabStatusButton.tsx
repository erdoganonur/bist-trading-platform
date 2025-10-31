import { useState } from 'react';
import { Button, Tag, Dropdown, Space, Typography } from 'antd';
import {
  LinkOutlined,
  DisconnectOutlined,
  CheckCircleOutlined,
  LoginOutlined,
  LogoutOutlined
} from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { useAlgoLabStore } from '@app/store';
import { useAlgoLabAuth } from '../hooks/useAlgoLabAuth';
import { AlgoLabAuthModal } from './AlgoLabAuthModal';

const { Text } = Typography;

/**
 * AlgoLab Broker Status Button and Dropdown Menu
 * Shows authentication status and provides quick actions
 */
export const AlgoLabStatusButton = () => {
  const { isAuthenticated, username, isAuthenticating } = useAlgoLabStore();
  const { logout } = useAlgoLabAuth();
  const [modalOpen, setModalOpen] = useState(false);

  const handleOpenModal = () => {
    setModalOpen(true);
  };

  const handleCloseModal = () => {
    setModalOpen(false);
  };

  const handleLogout = async () => {
    await logout();
  };

  const menuItems: MenuProps['items'] = isAuthenticated
    ? [
        {
          key: 'user',
          label: (
            <Space direction="vertical" size={0}>
              <Text strong>AlgoLab</Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                {username}
              </Text>
            </Space>
          ),
          disabled: true,
        },
        { type: 'divider' },
        {
          key: 'logout',
          label: 'Çıkış Yap',
          icon: <LogoutOutlined />,
          onClick: handleLogout,
        },
      ]
    : [
        {
          key: 'login',
          label: 'Broker Girişi',
          icon: <LoginOutlined />,
          onClick: handleOpenModal,
        },
      ];

  return (
    <>
      <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
        <Button
          type="text"
          loading={isAuthenticating}
          style={{ color: 'white', height: '40px' }}
        >
          <Space>
            {isAuthenticated ? (
              <>
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                <Text style={{ color: 'white' }}>AlgoLab</Text>
              </>
            ) : (
              <>
                <DisconnectOutlined style={{ color: '#ff4d4f' }} />
                <Text style={{ color: 'white' }}>Broker</Text>
              </>
            )}
          </Space>
        </Button>
      </Dropdown>

      <AlgoLabAuthModal
        open={modalOpen}
        onClose={handleCloseModal}
        onSuccess={handleCloseModal}
      />
    </>
  );
};

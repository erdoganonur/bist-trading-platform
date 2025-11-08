import React, { type ReactNode, useState } from 'react';
import {
  CContainer,
  CHeader,
  CHeaderBrand,
  CHeaderNav,
  CHeaderToggler,
  CNavbarNav,
  CNavItem,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem,
  CDropdownDivider,
  CBadge,
  CAvatar,
  CSidebar,
  CSidebarBrand,
  CSidebarNav,
  CSidebarToggler,
} from '@coreui/react';
import { cilBell, cilMenu, cilAccountLogout, cilSettings, cilUser } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { useAuthStore } from '@app/store';
import { useNavigate } from 'react-router-dom';
import { AlgoLabStatusButton } from '@features/broker/components';

export interface DashboardLayoutProps {
  children: ReactNode;
  sider?: ReactNode;
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children, sider }) => {
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const { user, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <div className="min-vh-100">
      {/* Sidebar */}
      {sider && (
        <CSidebar
          position="fixed"
          visible={sidebarVisible}
          onVisibleChange={(visible) => setSidebarVisible(visible)}
        >
          <CSidebarBrand className="d-none d-md-flex">
            <div className="sidebar-brand-full">
              <span className="fw-bold">BIST Trading</span>
            </div>
            <div className="sidebar-brand-narrow">
              <span className="fw-bold">BT</span>
            </div>
          </CSidebarBrand>
          <CSidebarNav>{sider}</CSidebarNav>
          <CSidebarToggler
            className="d-none d-lg-flex"
            onClick={() => setSidebarVisible(!sidebarVisible)}
          />
        </CSidebar>
      )}

      {/* Wrapper */}
      <div className="wrapper d-flex flex-column min-vh-100">
        {/* Header */}
        <CHeader position="sticky" className="mb-4">
          <CContainer fluid>
            {sider && (
              <CHeaderToggler
                className="ps-1"
                onClick={() => setSidebarVisible(!sidebarVisible)}
              >
                <CIcon icon={cilMenu} size="lg" />
              </CHeaderToggler>
            )}

            <CHeaderBrand className={sider ? "mx-auto d-md-none" : "me-auto"}>
              <span className="fw-bold">BIST Trading</span>
            </CHeaderBrand>

            <CHeaderNav className="d-none d-md-flex me-auto">
              <CNavItem>
                <span className="nav-link fw-bold text-primary">Trading Cockpit</span>
              </CNavItem>
            </CHeaderNav>

            <CHeaderNav>
              {/* AlgoLab Status */}
              <CNavItem>
                <AlgoLabStatusButton />
              </CNavItem>

              {/* Notifications */}
              <CNavItem>
                <div className="nav-link position-relative">
                  <CIcon icon={cilBell} size="lg" />
                  <CBadge
                    color="danger"
                    position="top-end"
                    shape="rounded-pill"
                    className="position-absolute top-0 start-100 translate-middle"
                  >
                    0
                  </CBadge>
                </div>
              </CNavItem>

              {/* User Dropdown */}
              <CNavItem>
                <CDropdown variant="nav-item" alignment="end">
                  <CDropdownToggle placement="bottom-end" className="py-0" caret={false}>
                    <CAvatar color="primary" textColor="white" size="md">
                      {user?.username?.[0]?.toUpperCase() || 'U'}
                    </CAvatar>
                  </CDropdownToggle>
                  <CDropdownMenu className="pt-0">
                    <CDropdownItem header className="bg-light fw-semibold py-2">
                      Account
                    </CDropdownItem>
                    <CDropdownItem onClick={() => navigate('/profile')}>
                      <CIcon icon={cilUser} className="me-2" />
                      Profile
                    </CDropdownItem>
                    <CDropdownItem onClick={() => navigate('/settings')}>
                      <CIcon icon={cilSettings} className="me-2" />
                      Settings
                    </CDropdownItem>
                    <CDropdownDivider />
                    <CDropdownItem onClick={handleLogout}>
                      <CIcon icon={cilAccountLogout} className="me-2" />
                      Logout
                    </CDropdownItem>
                  </CDropdownMenu>
                </CDropdown>
              </CNavItem>
            </CHeaderNav>
          </CContainer>
        </CHeader>

        {/* Main Content */}
        <div className="body flex-grow-1 px-3">
          <CContainer fluid>{children}</CContainer>
        </div>
      </div>
    </div>
  );
};

export default DashboardLayout;

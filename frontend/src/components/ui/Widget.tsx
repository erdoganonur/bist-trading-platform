import React, { type ReactNode } from 'react';
import {
  CCard,
  CCardBody,
  CCardHeader,
  CDropdown,
  CDropdownToggle,
  CDropdownMenu,
  CDropdownItem,
  CButton,
  CSpinner,
} from '@coreui/react';
import { cilOptions, cilReload, cilFullscreen } from '@coreui/icons';
import CIcon from '@coreui/icons-react';

export interface WidgetMenuItem {
  key: string;
  label: string;
  onClick?: () => void;
  icon?: ReactNode;
}

export interface WidgetProps {
  title?: string;
  icon?: ReactNode;
  extra?: ReactNode;
  children: ReactNode;
  loading?: boolean;
  className?: string;
  onRefresh?: () => void;
  onExpand?: () => void;
  menuItems?: WidgetMenuItem[];
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
  const hasActions = onRefresh || onExpand || (menuItems && menuItems.length > 0);

  return (
    <CCard className={`shadow-sm ${className}`}>
      {title && (
        <CCardHeader className={`d-flex align-items-center justify-content-between ${headerClassName}`}>
          <div className="d-flex align-items-center gap-2">
            {icon && <span className="text-primary">{icon}</span>}
            <span className="fw-semibold small">{title}</span>
          </div>

          <div className="d-flex align-items-center gap-2">
            {extra}

            {hasActions && (
              <CDropdown variant="btn-group">
                <CDropdownToggle
                  color="light"
                  size="sm"
                  className="border-0 opacity-75"
                  caret={false}
                >
                  <CIcon icon={cilOptions} size="sm" />
                </CDropdownToggle>
                <CDropdownMenu>
                  {onRefresh && (
                    <CDropdownItem onClick={onRefresh}>
                      <CIcon icon={cilReload} className="me-2" size="sm" />
                      Refresh
                    </CDropdownItem>
                  )}
                  {onExpand && (
                    <CDropdownItem onClick={onExpand}>
                      <CIcon icon={cilFullscreen} className="me-2" size="sm" />
                      Expand
                    </CDropdownItem>
                  )}
                  {menuItems?.map((item) => (
                    <CDropdownItem key={item.key} onClick={item.onClick}>
                      {item.icon && <span className="me-2">{item.icon}</span>}
                      {item.label}
                    </CDropdownItem>
                  ))}
                </CDropdownMenu>
              </CDropdown>
            )}
          </div>
        </CCardHeader>
      )}

      <CCardBody className={bodyClassName}>
        {loading ? (
          <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
            <CSpinner color="primary" />
          </div>
        ) : (
          children
        )}
      </CCardBody>
    </CCard>
  );
};

export default Widget;

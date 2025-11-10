import React, { useState } from 'react';
import {
  CTable,
  CTableHead,
  CTableBody,
  CTableRow,
  CTableHeaderCell,
  CTableDataCell,
  CBadge,
  CButton,
  CSpinner,
  CModal,
  CModalHeader,
  CModalTitle,
  CModalBody,
  CModalFooter,
  CForm,
  CFormLabel,
  CFormInput,
} from '@coreui/react';
import { cilPencil, cilTrash, cilClock, cilX } from '@coreui/icons';
import CIcon from '@coreui/icons-react';
import { Widget, OrderSideBadge, TradingButton } from '@components/ui';
import { formatCurrency, formatNumber } from '@utils/formatters';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { brokerApi } from '@services/api';
import dayjs from 'dayjs';

interface PendingOrder {
  id: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  price: number;
  quantity: number;
  status: string;
  createdAt: string;
}

export const PendingOrdersWidget: React.FC = () => {
  const [modifyModalOpen, setModifyModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<PendingOrder | null>(null);
  const [modifyPrice, setModifyPrice] = useState<number>(0);
  const [modifyLot, setModifyLot] = useState<number>(0);
  const [toast, setToast] = useState<{ type: 'success' | 'danger'; message: string } | null>(null);
  const queryClient = useQueryClient();

  const { data: orders, isLoading, refetch } = useQuery({
    queryKey: ['pendingOrders'],
    queryFn: brokerApi.getPendingOrders,
    refetchInterval: 3000,
  });

  const cancelMutation = useMutation({
    mutationFn: (orderId: string) => brokerApi.cancelOrder(orderId),
    onSuccess: () => {
      setToast({ type: 'success', message: 'Order cancelled successfully' });
      queryClient.invalidateQueries({ queryKey: ['pendingOrders'] });
    },
    onError: (error: any) => {
      setToast({ type: 'danger', message: error.message || 'Failed to cancel order' });
    },
  });

  const modifyMutation = useMutation({
    mutationFn: ({ orderId, price, lot }: { orderId: string; price: number; lot: number }) =>
      brokerApi.modifyOrder(orderId, { price, lot }),
    onSuccess: () => {
      setToast({ type: 'success', message: 'Order modified successfully' });
      setModifyModalOpen(false);
      setSelectedOrder(null);
      queryClient.invalidateQueries({ queryKey: ['pendingOrders'] });
    },
    onError: (error: any) => {
      setToast({ type: 'danger', message: error.message || 'Failed to modify order' });
    },
  });

  const handleCancel = (order: PendingOrder) => {
    if (window.confirm(`Are you sure you want to cancel this ${order.side} order for ${order.symbol}?`)) {
      cancelMutation.mutate(order.id);
    }
  };

  const handleModify = (order: PendingOrder) => {
    setSelectedOrder(order);
    setModifyPrice(order.price);
    setModifyLot(order.quantity / 100);
    setModifyModalOpen(true);
  };

  const handleModifySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedOrder) return;
    modifyMutation.mutate({
      orderId: selectedOrder.id,
      price: modifyPrice,
      lot: modifyLot * 100,
    });
  };

  return (
    <>
      <Widget
        title="Pending Orders"
        icon={<CIcon icon={cilClock} />}
        onRefresh={refetch}
        extra={
          <CBadge color="warning">
            {orders?.length || 0} Order{orders?.length !== 1 ? 's' : ''}
          </CBadge>
        }
      >
        {isLoading ? (
          <div className="text-center py-4">
            <CSpinner color="primary" />
          </div>
        ) : !orders || orders.length === 0 ? (
          <div className="text-center py-4 text-muted">
            <CIcon icon={cilClock} size="3xl" className="mb-3 opacity-25" />
            <p>No pending orders</p>
          </div>
        ) : (
          <div className="table-responsive">
            <CTable small hover className="mb-0">
              <CTableHead>
                <CTableRow>
                  <CTableHeaderCell>Time</CTableHeaderCell>
                  <CTableHeaderCell>Symbol</CTableHeaderCell>
                  <CTableHeaderCell>Side</CTableHeaderCell>
                  <CTableHeaderCell>Type</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Price</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Quantity</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Value</CTableHeaderCell>
                  <CTableHeaderCell>Status</CTableHeaderCell>
                  <CTableHeaderCell className="text-end">Actions</CTableHeaderCell>
                </CTableRow>
              </CTableHead>
              <CTableBody>
                {orders.map((order: PendingOrder) => (
                  <CTableRow key={order.id}>
                    <CTableDataCell>
                      <span className="small text-muted">
                        {dayjs(order.createdAt).format('HH:mm:ss')}
                      </span>
                    </CTableDataCell>
                    <CTableDataCell>
                      <span className="fw-semibold text-primary">{order.symbol}</span>
                    </CTableDataCell>
                    <CTableDataCell>
                      <OrderSideBadge side={order.side} />
                    </CTableDataCell>
                    <CTableDataCell>
                      <CBadge color={order.type === 'LIMIT' ? 'info' : 'warning'}>
                        {order.type}
                      </CBadge>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      {order.type === 'LIMIT' ? (
                        <span className="font-monospace">{formatCurrency(order.price)}</span>
                      ) : (
                        <span className="text-muted">Market</span>
                      )}
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      {formatNumber(order.quantity)}
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      {order.type === 'LIMIT' && order.price > 0 ? (
                        <span className="font-monospace">{formatCurrency(order.price * order.quantity)}</span>
                      ) : (
                        <span className="text-muted">-</span>
                      )}
                    </CTableDataCell>
                    <CTableDataCell>
                      <CBadge color="warning">
                        <CIcon icon={cilClock} size="sm" className="me-1" />
                        {order.status}
                      </CBadge>
                    </CTableDataCell>
                    <CTableDataCell className="text-end">
                      <div className="d-flex gap-1 justify-content-end">
                        <CButton
                          color="primary"
                          variant="ghost"
                          size="sm"
                          onClick={() => handleModify(order)}
                          disabled={order.type === 'MARKET'}
                          title="Modify Order"
                        >
                          <CIcon icon={cilPencil} size="sm" />
                        </CButton>
                        <CButton
                          color="danger"
                          variant="ghost"
                          size="sm"
                          onClick={() => handleCancel(order)}
                          disabled={cancelMutation.isPending}
                          title="Cancel Order"
                        >
                          <CIcon icon={cilTrash} size="sm" />
                        </CButton>
                      </div>
                    </CTableDataCell>
                  </CTableRow>
                ))}
              </CTableBody>
            </CTable>
          </div>
        )}
      </Widget>

      {/* Modify Order Modal */}
      <CModal
        visible={modifyModalOpen}
        onClose={() => {
          setModifyModalOpen(false);
          setSelectedOrder(null);
        }}
      >
        <CModalHeader>
          <CModalTitle>Modify Order - {selectedOrder?.symbol}</CModalTitle>
        </CModalHeader>
        <CForm onSubmit={handleModifySubmit}>
          <CModalBody>
            <div className="mb-3">
              <CFormLabel>Price (₺)</CFormLabel>
              <CFormInput
                type="number"
                step="0.01"
                min="0"
                value={modifyPrice || ''}
                onChange={(e) => setModifyPrice(parseFloat(e.target.value) || 0)}
                required
              />
            </div>
            <div className="mb-3">
              <CFormLabel>
                Quantity (Lot)
                <small className="text-muted ms-2">1 lot = 100 shares</small>
              </CFormLabel>
              <CFormInput
                type="number"
                step="1"
                min="1"
                value={modifyLot || ''}
                onChange={(e) => setModifyLot(parseInt(e.target.value) || 0)}
                required
              />
            </div>
          </CModalBody>
          <CModalFooter>
            <CButton
              color="secondary"
              onClick={() => setModifyModalOpen(false)}
            >
              Cancel
            </CButton>
            <TradingButton
              action="modify"
              htmlType="submit"
              loading={modifyMutation.isPending}
            >
              Modify Order
            </TradingButton>
          </CModalFooter>
        </CForm>
      </CModal>
    </>
  );
};

export default PendingOrdersWidget;

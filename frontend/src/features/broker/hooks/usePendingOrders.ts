import { useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { brokerService } from '@services/api/broker.service';
import type { AlgoLabPendingOrder } from '@services/api/broker.service';

/**
 * Custom hook for managing pending orders
 * Fetches and manages AlgoLab pending orders with auto-refresh
 */
export const usePendingOrders = (autoRefresh = true, refreshInterval = 30000) => {
  const [orders, setOrders] = useState<AlgoLabPendingOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch pending orders from AlgoLab
   */
  const fetchOrders = useCallback(async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      setError(null);

      const response = await brokerService.getPendingOrders();
      const ordersList = response.data.orders || [];

      setOrders(ordersList);

      if (!silent && ordersList.length === 0) {
        message.info('Bekleyen emir bulunmamaktadır');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Emirler yüklenemedi';
      setError(errorMessage);

      if (!silent) {
        message.error(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Cancel an order
   */
  const cancelOrder = useCallback(async (orderId: string) => {
    try {
      setLoading(true);

      const response = await brokerService.cancelOrder(orderId);

      if (response.data.success) {
        message.success('Emir iptal edildi');
        // Refresh orders list
        await fetchOrders(true);
        return true;
      } else {
        throw new Error(response.data.message || 'Emir iptal edilemedi');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Emir iptal edilemedi';
      message.error(errorMessage);
      return false;
    } finally {
      setLoading(false);
    }
  }, [fetchOrders]);

  /**
   * Modify an order
   */
  const modifyOrder = useCallback(async (
    orderId: string,
    updates: { price?: number; lot?: number }
  ) => {
    try {
      setLoading(true);

      const response = await brokerService.modifyOrder(orderId, updates);

      if (response.data.success) {
        message.success('Emir güncellendi');
        // Refresh orders list
        await fetchOrders(true);
        return true;
      } else {
        throw new Error(response.data.message || 'Emir güncellenemedi');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Emir güncellenemedi';
      message.error(errorMessage);
      return false;
    } finally {
      setLoading(false);
    }
  }, [fetchOrders]);

  /**
   * Auto-refresh effect
   */
  useEffect(() => {
    if (autoRefresh) {
      // Initial fetch
      fetchOrders();

      // Set up interval
      const interval = setInterval(() => {
        fetchOrders(true); // Silent refresh
      }, refreshInterval);

      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval, fetchOrders]);

  return {
    orders,
    loading,
    error,
    fetchOrders,
    cancelOrder,
    modifyOrder,
  };
};

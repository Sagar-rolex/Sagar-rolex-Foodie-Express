import apiClient from './axios';
import { ApiResponse, Order, OrderRequest } from '../types';
import type { PageData } from '../types/admin';

export const orderApi = {
  placeOrder: async (data: OrderRequest): Promise<ApiResponse<Order>> => {
    const response = await apiClient.post('/orders/place', data);
    return response.data;
  },

  getMyOrders: async (): Promise<ApiResponse<Order[]>> => {
    const response = await apiClient.get('/orders');
    return response.data;
  },

  getOrderHistory: async (params: { page?: number; size?: number; status?: string } = {}): Promise<ApiResponse<PageData<Order>>> => {
    const response = await apiClient.get('/orders/history', { params });
    return response.data;
  },

  getTracking: async (orderId: number): Promise<ApiResponse<{ orderId: number; currentStatus: Order['status']; latestUpdatedAt: string; cancellationReason: string | null; paymentStatus: string; paymentMethod: string | null; paymentReference: string | null; timeline: { status: Order['status']; changedAt: string; note: string | null }[] }>> => {
    const response = await apiClient.get(`/orders/${orderId}/tracking`);
    return response.data;
  },

  reorder: async (orderId: number): Promise<ApiResponse<{ currentSubtotal: number; addedItems: string[]; unavailableItems: string[] }>> => {
    const response = await apiClient.post(`/orders/${orderId}/reorder`);
    return response.data;
  },

  getOrderById: async (orderId: number): Promise<ApiResponse<Order>> => {
    const response = await apiClient.get(`/orders/${orderId}`);
    return response.data;
  },

  cancelOrder: async (orderId: number, reason?: string): Promise<ApiResponse<Order>> => {
    const response = await apiClient.patch(`/orders/${orderId}/cancel`, { reason });
    return response.data;
  },
};

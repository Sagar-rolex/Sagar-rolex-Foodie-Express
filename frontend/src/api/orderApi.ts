import apiClient from './axios';
import { ApiResponse, Order, OrderRequest } from '../types';

export const orderApi = {
  placeOrder: async (data: OrderRequest): Promise<ApiResponse<Order>> => {
    const response = await apiClient.post('/orders/place', data);
    return response.data;
  },

  getMyOrders: async (): Promise<ApiResponse<Order[]>> => {
    const response = await apiClient.get('/orders');
    return response.data;
  },

  getOrderById: async (orderId: number): Promise<ApiResponse<Order>> => {
    const response = await apiClient.get(`/orders/${orderId}`);
    return response.data;
  },

  cancelOrder: async (orderId: number): Promise<ApiResponse<Order>> => {
    const response = await apiClient.patch(`/orders/${orderId}/cancel`);
    return response.data;
  },
};
import apiClient from './axios';
import { ApiResponse, Bill } from '../types';

export const billApi = {
  getBillByOrder: async (orderId: number): Promise<ApiResponse<Bill>> => {
    const response = await apiClient.get(`/bills/order/${orderId}`);
    return response.data;
  },

  getMyBills: async (): Promise<ApiResponse<Bill[]>> => {
    const response = await apiClient.get('/bills/my');
    return response.data;
  },

  getBillById: async (billId: number): Promise<ApiResponse<Bill>> => {
    const response = await apiClient.get(`/bills/${billId}`);
    return response.data;
  },
};

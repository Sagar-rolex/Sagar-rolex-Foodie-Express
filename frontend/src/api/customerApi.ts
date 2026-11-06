import apiClient from './axios';
import { ApiResponse, Customer } from '../types';

export const customerApi = {
  getProfile: async (): Promise<ApiResponse<Customer>> => {
    const response = await apiClient.get('/customers/me');
    return response.data;
  },

  updateProfile: async (data: { fullName?: string; phone?: string; address?: string }): Promise<ApiResponse<Customer>> => {
    const response = await apiClient.put('/customers/me', data);
    return response.data;
  },
};
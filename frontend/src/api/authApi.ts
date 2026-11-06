import apiClient from './axios';
import { ApiResponse, LoginRequest, LoginResponse, RegisterRequest } from '../types';

export const authApi = {
  register: async (data: RegisterRequest): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.post('/auth/register', data);
    return response.data;
  },

  login: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.post('/auth/login', data);
    return response.data;
  },

  logout: async (): Promise<ApiResponse<string>> => {
    const response = await apiClient.post('/auth/logout');
    return response.data;
  },

  getMe: async (): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.get('/auth/me');
    return response.data;
  },

  adminLogin: async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    const response = await apiClient.post('/admin/login', data);
    return response.data;
  },

  adminLogout: async (): Promise<ApiResponse<string>> => {
    const response = await apiClient.post('/admin/logout');
    return response.data;
  },
};
import apiClient from './axios';
import { ApiResponse, Restaurant, FoodItem } from '../types';

export const restaurantApi = {
  getAll: async (): Promise<ApiResponse<Restaurant[]>> => {
    const response = await apiClient.get('/restaurants');
    return response.data;
  },

  getById: async (id: number): Promise<ApiResponse<Restaurant>> => {
    const response = await apiClient.get(`/restaurants/${id}`);
    return response.data;
  },

  getMenuItems: async (restaurantId: number): Promise<ApiResponse<FoodItem[]>> => {
    const response = await apiClient.get(`/restaurants/${restaurantId}/items`);
    return response.data;
  },

  getFoodItem: async (id: number): Promise<ApiResponse<FoodItem>> => {
    const response = await apiClient.get(`/food-items/${id}`);
    return response.data;
  },
};
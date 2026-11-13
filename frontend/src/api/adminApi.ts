import apiClient from './axios';
import type { ApiResponse, Restaurant, RestaurantRequest, FoodItem, FoodItemRequest, Order, OrderStatus } from '../types';
import type { AdminDashboardData, AdminOrder, AdminCustomer, PageData } from '../types/admin';

export const adminApi = {
  getDashboard: async (params: { from?: string; to?: string; grouping?: string } = {}): Promise<ApiResponse<AdminDashboardData>> => (await apiClient.get('/admin/dashboard', { params })).data,
  getRestaurants: async (): Promise<ApiResponse<Restaurant[]>> => (await apiClient.get('/admin/restaurants')).data,
  createRestaurant: async (data: RestaurantRequest): Promise<ApiResponse<Restaurant>> => (await apiClient.post('/admin/restaurants', data)).data,
  updateRestaurant: async (id: number, data: RestaurantRequest): Promise<ApiResponse<Restaurant>> => (await apiClient.put(`/admin/restaurants/${id}`, data)).data,
  deleteRestaurant: async (id: number): Promise<ApiResponse<string>> => (await apiClient.delete(`/admin/restaurants/${id}`)).data,
  setRestaurantActive: async (id: number, active: boolean): Promise<ApiResponse<Restaurant>> => (await apiClient.patch(`/admin/restaurants/${id}/active`, { active })).data,

  getFoodItems: async (params: { search?: string; restaurantId?: number | null; available?: boolean | null; page?: number; size?: number } = {}): Promise<ApiResponse<PageData<FoodItem>>> =>
    (await apiClient.get('/admin/food-items', { params })).data,
  uploadFoodImage: async (file: File): Promise<ApiResponse<{ imagePath: string }>> => {
    const form = new FormData();
    form.append('file', file);
    return (await apiClient.post('/admin/uploads/foods', form, { headers: { 'Content-Type': 'multipart/form-data' } })).data;
  },
  uploadRestaurantBanner: async (file: File): Promise<ApiResponse<{ bannerImagePath: string }>> => {
    const form = new FormData();
    form.append('file', file);
    return (await apiClient.post('/admin/uploads/restaurant-banners', form, { headers: { 'Content-Type': 'multipart/form-data' } })).data;
  },
  createFoodItem: async (restaurantId: number, data: FoodItemRequest): Promise<ApiResponse<FoodItem>> => (await apiClient.post(`/admin/restaurants/${restaurantId}/items`, data)).data,
  updateFoodItem: async (id: number, data: FoodItemRequest): Promise<ApiResponse<FoodItem>> => (await apiClient.put(`/admin/food-items/${id}`, data)).data,
  deleteFoodItem: async (id: number): Promise<ApiResponse<string>> => (await apiClient.delete(`/admin/food-items/${id}`)).data,

  getAllOrders: async (params: { search?: string; status?: string; paymentStatus?: string; restaurantId?: number | null; from?: string; to?: string; page?: number; size?: number } = {}): Promise<ApiResponse<PageData<AdminOrder>>> =>
    (await apiClient.get('/admin/orders', { params })).data,
  updateOrderStatus: async (orderId: number, status: OrderStatus): Promise<ApiResponse<Order>> => (await apiClient.patch(`/admin/orders/${orderId}/status`, { status })).data,

  getCustomers: async (params: { search?: string; active?: boolean | null; page?: number; size?: number } = {}): Promise<ApiResponse<PageData<AdminCustomer>>> =>
    (await apiClient.get('/admin/customers', { params })).data,
  setCustomerActive: async (id: number, active: boolean): Promise<ApiResponse<AdminCustomer>> =>
    (await apiClient.patch(`/admin/customers/${id}/active`, { active })).data,
};

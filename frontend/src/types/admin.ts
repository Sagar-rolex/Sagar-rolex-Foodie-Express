import type { OrderStatus } from './index';

export interface AdminRecentOrder {
  id: number;
  restaurantName: string | null;
  totalAmount: number | null;
  status: OrderStatus;
}

export interface AdminDashboardData {
  totalRestaurants: number | null;
  totalOrders: number | null;
  totalRevenue: number | null;
  totalCustomers: number | null;
  availableFoodItems: number | null;
  pendingOrders: number | null;
  recentOrders: AdminRecentOrder[];
  ordersOverTime: { date: string; value: number }[];
  revenueOverTime: { date: string; value: number }[];
  bestSellingDishes: { name: string; quantity: number }[];
  unavailableMetrics: string[];
}

export interface PageData<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminOrderItem { name: string; quantity: number; unitPrice: number }
export interface AdminOrder {
  id: number;
  customerName: string;
  customerEmail: string | null;
  customerPhone: string | null;
  restaurantName: string;
  orderTime: string;
  subtotal: number;
  totalAmount: number;
  currency: string;
  status: OrderStatus;
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  paymentMethod: string | null;
  paymentReference: string | null;
  items: AdminOrderItem[];
}

export interface AdminCustomer {
  id: number;
  fullName: string;
  username: string;
  email: string | null;
  phone: string | null;
  createdAt: string | null;
  active: boolean;
  orderCount: number;
}

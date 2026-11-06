export interface User {
  id: number;
  username: string;
  email: string;
  role: 'CUSTOMER' | 'ADMIN';
  sessionToken?: string;
}

export interface Customer {
  id: number;
  fullName: string;
  phone: string;
  address: string;
  user: User;
}

export interface Restaurant {
  id: number;
  name: string;
  address: string;
  cuisineType: string;
  description: string;
  imagePath?: string | null;
  bannerImagePath?: string | null;
  active: boolean;
  rating?: number;
  deliveryTime?: number;
  foodItems?: FoodItem[];
}

export interface FoodItem {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  imagePath?: string | null;
  available: boolean;
  restaurant: Restaurant;
}

export interface CartItem {
  id: number;
  foodItem: FoodItem;
  quantity: number;
  priceAtAdd?: number | null;
}

export interface Cart {
  id: number;
  items: CartItem[];
  customer: Customer | null;
}

export type OrderStatus =
  | 'PLACED'
  | 'CONFIRMED'
  | 'PREPARING'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderItem {
  id: number;
  foodItem: FoodItem;
  quantity: number;
  priceAtOrder: number;
  itemNameAtOrder?: string | null;
}

export interface Order {
  id: number;
  customer: Customer;
  restaurant: Restaurant;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  orderTime: string;
  deliveryAddress: string;
}

export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';

export interface Bill {
  id: number;
  order: Order;
  totalAmount: number;
  tax: number;
  deliveryFee?: number | null;
  discount?: number | null;
  couponCode?: string | null;
  grandTotal: number;
  paymentStatus: PaymentStatus;
  generatedAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface LoginRequest {
  username?: string;
  email?: string;
  password: string;
}

export interface LoginResponse {
  userId: number;
  username: string;
  role: string;
  sessionToken: string;
  message: string;
}

export interface RegisterRequest {
  username?: string;
  password: string;
  email: string;
  fullName: string;
  phone?: string;
  address?: string;
}

export interface CartRequest {
  foodItemId: number;
  quantity: number;
}

export interface OrderRequest {
  deliveryAddress: string;
  couponCode?: string;
}

export interface RestaurantRequest {
  name: string;
  address: string;
  cuisineType: string;
  description: string;
  imagePath?: string;
  bannerImagePath?: string;
}

export interface FoodItemRequest {
  name: string;
  description: string;
  price: number;
  category: string;
  imagePath?: string;
  available?: boolean;
}

import apiClient from './axios';
import { ApiResponse, Cart, CartItem, CartRequest, FoodItem } from '../types';
import { restaurantApi } from './restaurantApi';

const STORAGE_KEY = 'foodie-express-guest-cart:v1';
const MAX_QUANTITY = 99;
type SavedItem = { foodItemId: number; quantity: number; foodItem: FoodItem; priceAtSave?: number; priceChangeNotified?: boolean };
type CartPayload = { cart: Cart; total: number };
type MergePayload = CartPayload & { mergedFoodItemIds: number[]; skippedFoodItemIds: number[]; priceChangedFoodItemIds: number[] };
const notifyCartUpdated = () => window.dispatchEvent(new Event('cart:updated'));
const hasSession = () => Boolean(localStorage.getItem('sessionToken'));
let mergeInFlight: Promise<MergePayload | null> | null = null;
let guestMutationChain: Promise<void> = Promise.resolve();

function serializeGuestMutation<T>(work: () => Promise<T>): Promise<T> {
  const next = guestMutationChain.then(work, work);
  guestMutationChain = next.then(() => undefined, () => undefined);
  return next;
}

function readSaved(): SavedItem[] {
  let raw: string | null;
  try { raw = localStorage.getItem(STORAGE_KEY); } catch { return []; }
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) throw new Error('invalid guest cart');
    return parsed.filter((x): x is SavedItem => Number.isInteger(x?.foodItemId) && x.foodItemId > 0
      && Number.isInteger(x?.quantity) && x.quantity > 0 && x.quantity <= MAX_QUANTITY
      && x.foodItem && x.foodItem.id === x.foodItemId && typeof x.foodItem.name === 'string'
      && Number.isFinite(Number(x.foodItem.price)) && Number(x.foodItem.price) >= 0
      && Number.isInteger(x.foodItem.restaurant?.id) && typeof x.foodItem.restaurant?.active === 'boolean');
  } catch {
    try { localStorage.removeItem(STORAGE_KEY); } catch { /* Storage may be disabled. */ }
    return [];
  }
}

function writeSaved(items: SavedItem[]) {
  try {
    if (items.length) localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    throw new Error('Your browser could not save the cart. Check local storage permissions and retry.');
  }
}

function asGuestCart(items: SavedItem[]): CartPayload {
  const cartItems: CartItem[] = items.map(item => ({ id: -item.foodItemId, foodItem: item.foodItem,
    quantity: item.quantity, priceAtAdd: item.priceAtSave ?? item.foodItem.price }));
  const subtotalPaise = cartItems.reduce((sum, item) => sum + Math.round(Number(item.foodItem.price) * 100) * item.quantity, 0);
  return { cart: { id: 0, customer: null, items: cartItems }, total: subtotalPaise / 100 };
}

async function getGuestCart(): Promise<CartPayload> {
  const saved = readSaved();
  const refreshed = await Promise.all(saved.map(async item => {
    try {
      const result = await restaurantApi.getFoodItem(item.foodItemId);
      const food = result.data;
      const savedPrice = item.priceAtSave ?? item.foodItem.price;
      const changed = Math.round(Number(food.price) * 100) !== Math.round(Number(savedPrice) * 100);
      if (changed && !item.priceChangeNotified) window.dispatchEvent(new CustomEvent('cart:price-changed', { detail: 1 }));
      return { ...item, priceAtSave: savedPrice, priceChangeNotified: item.priceChangeNotified || changed, foodItem: food };
    } catch (error: any) {
      if (error.response?.status === 404) return { ...item, foodItem: { ...item.foodItem, available: false } };
      // Retain the last snapshot when the server is temporarily unavailable.
      return item;
    }
  }));

  // Reconcile with edits made while validation requests were in flight.
  const latest = readSaved();
  const latestById = new Map(latest.map(item => [item.foodItemId, item]));
  const originalIds = new Set(saved.map(item => item.foodItemId));
  const safeRefreshed: SavedItem[] = [];
  refreshed.forEach(item => {
    const current = latestById.get(item.foodItemId);
    if (current) safeRefreshed.push({ ...item, quantity: current.quantity, priceAtSave: current.priceAtSave ?? item.priceAtSave,
      priceChangeNotified: current.priceChangeNotified || item.priceChangeNotified });
  });
  safeRefreshed.push(...latest.filter(item => !originalIds.has(item.foodItemId)));
  writeSaved(safeRefreshed);
  return asGuestCart(safeRefreshed);
}

function mergeGuestCart(): Promise<MergePayload | null> {
  if (mergeInFlight) return mergeInFlight;
  mergeInFlight = performGuestMerge().finally(() => { mergeInFlight = null; });
  return mergeInFlight;
}

async function performGuestMerge(): Promise<MergePayload | null> {
  const items = readSaved();
  if (!hasSession() || items.length === 0) return null;
  const response = await apiClient.post<ApiResponse<MergePayload>>('/cart/merge', {
    items: items.map(({ foodItemId, quantity, priceAtSave, foodItem }) => ({
      foodItemId, quantity, priceAtSave: priceAtSave ?? foodItem.price,
    })),
  });
  const result = response.data.data;
  const mergedIds = new Set(result.mergedFoodItemIds);
  writeSaved(items.filter(item => !mergedIds.has(item.foodItemId)));
  if (result.skippedFoodItemIds.length) {
    window.dispatchEvent(new CustomEvent('cart:merge-skipped', { detail: result.skippedFoodItemIds.length }));
  }
  if (result.priceChangedFoodItemIds.length) {
    window.dispatchEvent(new CustomEvent('cart:price-changed', { detail: result.priceChangedFoodItemIds.length }));
  }
  notifyCartUpdated();
  return result;
}

async function getAuthenticatedCart(): Promise<CartPayload> {
  let failedMerge = false;
  if (readSaved().length) {
    try { await mergeGuestCart(); } catch { failedMerge = true; }
  }
  if (failedMerge) return getGuestCart();
  const response = await apiClient.get<ApiResponse<CartPayload>>('/cart');
  const data = response.data.data;
  const leftovers = readSaved();
  if (!leftovers.length) return data;
  const existingIds = new Set(data.cart.items.map(item => item.foodItem.id));
  const additional = asGuestCart(leftovers).cart.items.filter(item => !existingIds.has(item.foodItem.id));
  additional.forEach(item => { item.foodItem.available = false; });
  const mergedItems = [...data.cart.items, ...additional];
  const totalPaise = mergedItems.reduce((sum, item) => sum + Math.round(Number(item.foodItem.price) * 100) * item.quantity, 0);
  return { cart: { ...data.cart, items: mergedItems }, total: totalPaise / 100 };
}

export const cartApi = {
  getCart: async (): Promise<ApiResponse<CartPayload>> => {
    const data = hasSession() ? await getAuthenticatedCart() : await getGuestCart();
    return { success: true, message: 'Cart retrieved', data };
  },

  addItem: async (data: CartRequest): Promise<ApiResponse<CartPayload>> => {
    if (!Number.isInteger(data.quantity) || data.quantity < 1 || data.quantity > MAX_QUANTITY) throw new Error('Quantity must be between 1 and 99');
    if (!hasSession()) {
      return serializeGuestMutation(async () => {
        const result = await restaurantApi.getFoodItem(data.foodItemId);
        const foodItem = result.data;
        if (!foodItem.available || !foodItem.restaurant.active) throw new Error('This food item is no longer available');
        const items = readSaved();
        if (items.some(item => item.foodItem.restaurant.id !== foodItem.restaurant.id)) {
          throw new Error('Please order from one restaurant at a time. Remove the other restaurant items first.');
        }
        const existing = items.find(item => item.foodItemId === foodItem.id);
        const quantity = (existing?.quantity || 0) + data.quantity;
        if (quantity > MAX_QUANTITY) throw new Error('You can add up to 99 of each dish');
        if (existing) { existing.quantity = quantity; existing.foodItem = foodItem; existing.priceAtSave = foodItem.price; existing.priceChangeNotified = false; }
        else items.push({ foodItemId: foodItem.id, quantity, foodItem, priceAtSave: foodItem.price });
        writeSaved(items);
        notifyCartUpdated();
        return { success: true, message: 'Item added to cart', data: asGuestCart(items) };
      });
    }
    await mergeGuestCart();
    const response = await apiClient.post<ApiResponse<CartPayload>>('/cart/add', data);
    notifyCartUpdated();
    return response.data;
  },

  updateItem: async (cartItemId: number, quantity: number): Promise<ApiResponse<CartPayload>> => {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > MAX_QUANTITY) throw new Error('Quantity must be between 1 and 99');
    if (cartItemId < 0 || !hasSession()) {
      return serializeGuestMutation(async () => {
        const id = Math.abs(cartItemId);
        const items = readSaved();
        const target = items.find(item => item.foodItemId === id);
        if (!target) throw new Error('Cart item is no longer available');
        target.quantity = quantity;
        writeSaved(items);
        notifyCartUpdated();
        return { success: true, message: 'Cart updated', data: asGuestCart(items) };
      });
    }
    await mergeGuestCart();
    const response = await apiClient.put<ApiResponse<CartPayload>>(`/cart/items/${cartItemId}?quantity=${quantity}`);
    notifyCartUpdated();
    return response.data;
  },

  removeItem: async (cartItemId: number): Promise<ApiResponse<CartPayload>> => {
    if (cartItemId < 0 || !hasSession()) {
      return serializeGuestMutation(async () => {
        const id = Math.abs(cartItemId);
        const items = readSaved().filter(item => item.foodItemId !== id);
        writeSaved(items);
        notifyCartUpdated();
        return { success: true, message: 'Item removed', data: asGuestCart(items) };
      });
    }
    await mergeGuestCart();
    const response = await apiClient.delete<ApiResponse<CartPayload>>(`/cart/items/${cartItemId}`);
    notifyCartUpdated();
    return response.data;
  },

  clearCart: async (): Promise<ApiResponse<string>> => {
    if (hasSession()) await apiClient.delete('/cart/clear');
    await serializeGuestMutation(async () => { writeSaved([]); return undefined; });
    notifyCartUpdated();
    return { success: true, message: 'Cart cleared', data: '' };
  },

  mergeGuestCart,
};

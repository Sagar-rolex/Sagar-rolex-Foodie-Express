import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { cartApi } from '../api/cartApi';
import type { Cart as CartModel } from '../types';
import { toast } from 'sonner';
import { Trash2, Plus, Minus, ShoppingCart } from 'lucide-react';
import { calculateCartTotals, formatCurrency } from '../lib/utils';
import SafeImage from '../components/SafeImage';
import { useAuth } from '../contexts/AuthContext';
import CouponPanel from '../components/CouponPanel';
import type { CouponTotals } from '../api/couponApi';

export default function Cart() {
  const [cart, setCart] = useState<CartModel | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [updating, setUpdating] = useState<Record<number, boolean>>({});
  const [couponTotals, setCouponTotals] = useState<CouponTotals|null>(null);
  const navigate = useNavigate();
  const { isAdmin } = useAuth();

  const fetchCart = useCallback(async () => {
    try {
      const response = await cartApi.getCart();
      setCart(response.data.cart);
      setLoadError('');
    } catch (error: any) {
      setLoadError(error.response?.data?.message || error.message || 'Could not load your cart. Check your connection and retry.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void fetchCart(); }, [fetchCart]);
  useEffect(() => {
    const skipped = (event: Event) => {
      const count = (event as CustomEvent<number>).detail;
      if (count) toast.warning(`${count} saved item${count === 1 ? '' : 's'} could not be added. Review your cart before checkout.`);
    };
    const priceChanged = (event: Event) => {
      const count = (event as CustomEvent<number>).detail;
      if (count) toast.info(`The current catalog price changed for ${count} saved cart item${count === 1 ? '' : 's'}.`);
    };
    window.addEventListener('cart:merge-skipped', skipped);
    window.addEventListener('cart:price-changed', priceChanged);
    return () => {
      window.removeEventListener('cart:merge-skipped', skipped);
      window.removeEventListener('cart:price-changed', priceChanged);
    };
  }, []);

  const updateQuantity = async (itemId: number, quantity: number) => {
    setUpdating(prev => ({ ...prev, [itemId]: true }));
    try {
      const response = await cartApi.updateItem(itemId, quantity);
      setCart(response.data.cart);
    } catch (error: any) {
      toast.error(error.response?.data?.message || error.message || 'Failed to update cart');
    } finally {
      setUpdating(prev => ({ ...prev, [itemId]: false }));
    }
  };

  const removeItem = async (itemId: number) => {
    setUpdating(prev => ({ ...prev, [itemId]: true }));
    try {
      const response = await cartApi.removeItem(itemId);
      setCart(response.data.cart);
      toast.success('Item removed from cart');
    } catch (error: any) {
      toast.error(error.response?.data?.message || error.message || 'Failed to remove item');
    } finally {
      setUpdating(prev => ({ ...prev, [itemId]: false }));
    }
  };

  const clearCart = async () => {
    try {
      await cartApi.clearCart();
      setCart({ id: 0, customer: null, items: [] });
      toast.success('Cart cleared');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to clear cart');
    }
  };

  if (isAdmin) return <Navigate to="/admin/dashboard" replace />;
  if (loading) return <div className="max-w-4xl mx-auto px-4 py-8"><div className="bg-gray-200 h-64 rounded-lg animate-pulse" /></div>;
  if (loadError && !cart) return <div className="min-h-[70vh] flex flex-col items-center justify-center px-4 text-center">
    <p className="text-gray-700 mb-4">{loadError}</p>
    <button onClick={() => { setLoading(true); void fetchCart(); }} className="bg-primary text-white px-5 py-2 rounded-lg">Retry</button>
  </div>;
  if (!cart || cart.items.length === 0) return <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
    <div className="text-center"><ShoppingCart size={48} className="mx-auto text-gray-400 mb-6" />
      <h2 className="text-2xl font-bold text-gray-900 mb-2">Your cart is empty</h2>
      <p className="text-gray-600 mb-6">Add some delicious food to your cart!</p>
      <Link to="/restaurants" className="inline-block bg-primary text-white px-6 py-3 rounded-lg font-bold hover:bg-primary/90 transition">Browse Restaurants</Link>
    </div>
  </div>;

  const totals = calculateCartTotals(cart.items);
  const unavailable = cart.items.filter(item => !item.foodItem.available || !item.foodItem.restaurant?.active);
  const restaurantIds = new Set(cart.items.map(item => item.foodItem.restaurant?.id));
  const canCheckout = unavailable.length === 0 && restaurantIds.size === 1;
  const itemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

  return <div className="min-h-[80vh] bg-gray-50 pb-28 md:pb-8">
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex items-center justify-between gap-3 mb-8">
        <h1 className="text-3xl font-bold flex items-center"><ShoppingCart className="mr-3" />Your Cart</h1>
        <button onClick={() => void clearCart()} className="text-red-600 hover:text-red-700 font-medium">Clear Cart</button>
      </div>
      {loadError && <div role="status" className="mb-4 rounded-lg bg-amber-50 border border-amber-200 p-3 text-sm text-amber-800">Your account cart could not be synchronized. Your saved guest items are still shown; retry by refreshing.</div>}
      <div className="bg-white rounded-xl shadow-lg overflow-hidden mb-6 divide-y">
        {cart.items.map(item => {
          const busy = Boolean(updating[item.id]);
          const itemAvailable = item.foodItem.available && item.foodItem.restaurant?.active;
          const priceChanged = item.priceAtAdd != null
            && Math.round(Number(item.priceAtAdd) * 100) !== Math.round(Number(item.foodItem.price) * 100);
          return <article key={item.id} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 p-4 sm:p-6">
            <div className="flex items-center gap-4 min-w-0">
              <SafeImage imagePath={item.foodItem.imagePath} alt={item.foodItem.name} className="h-20 w-20 shrink-0 rounded-lg" />
              <div className="min-w-0">
                <h2 className="font-semibold text-gray-900 truncate">{item.foodItem.name}</h2>
                <p className="text-gray-500 text-sm">{item.foodItem.category}</p>
                <p className="text-gray-500 text-sm">{formatCurrency(item.foodItem.price)} each</p>
                {priceChanged && <p className="text-sm text-amber-700 mt-1">Price changed from {formatCurrency(Number(item.priceAtAdd))}. Review the updated total.</p>}
                {!itemAvailable && <p className="text-sm text-red-600 mt-1">Unavailable now. Remove this item to continue.</p>}
              </div>
            </div>
            <div className="flex items-center justify-between sm:justify-end gap-5">
              <div className="flex items-center gap-2">
                <button aria-label={`Decrease ${item.foodItem.name} quantity`} onClick={() => void updateQuantity(item.id, item.quantity - 1)} disabled={item.quantity <= 1 || busy || !itemAvailable}
                  className="p-2 bg-gray-50 rounded hover:bg-gray-100 disabled:opacity-40"><Minus size={18} /></button>
                <span className="w-8 text-center font-medium" aria-live="polite">{item.quantity}</span>
                <button aria-label={`Increase ${item.foodItem.name} quantity`} onClick={() => void updateQuantity(item.id, item.quantity + 1)} disabled={busy || item.quantity >= 99 || !itemAvailable}
                  className="p-2 bg-gray-50 rounded hover:bg-gray-100 disabled:opacity-40"><Plus size={18} /></button>
              </div>
              <strong className="text-lg text-primary min-w-24 text-right">{formatCurrency(item.foodItem.price * item.quantity)}</strong>
              <button aria-label={`Remove ${item.foodItem.name}`} onClick={() => void removeItem(item.id)} disabled={busy}
                className="p-2 text-red-500 hover:bg-red-50 rounded disabled:opacity-40"><Trash2 size={20} /></button>
            </div>
          </article>;
        })}
      </div>

      <section className="bg-white rounded-xl shadow-lg p-5 sm:p-6">
        <h2 className="text-xl font-bold mb-5">Order Summary</h2>
        <CouponPanel key={cart.items.map(i=>`${i.id}:${i.quantity}:${i.foodItem.price}`).join('|')} onApplied={setCouponTotals} />
        <div className="space-y-3 text-gray-600">
          <div className="flex justify-between"><span>Subtotal</span><span>{formatCurrency(couponTotals?.subtotal ?? totals.subtotal)}</span></div>
          <div className="flex justify-between"><span>Tax (10%)</span><span>{formatCurrency(couponTotals?.tax ?? totals.tax)}</span></div>
          <div className="flex justify-between"><span>Delivery Fee</span><span>{formatCurrency(couponTotals?.deliveryFee ?? totals.deliveryFee)}</span></div>
          {couponTotals&&<div className="flex justify-between text-green-700"><span>Discount ({couponTotals.code})</span><span>−{formatCurrency(couponTotals.discount)}</span></div>}
          <hr />
          <div className="flex justify-between text-xl font-bold text-gray-900"><span>Total</span><span className="text-primary">{formatCurrency(couponTotals?.grandTotal ?? totals.grandTotal)}</span></div>
          <p className="text-xs text-gray-500">{itemCount} item{itemCount === 1 ? '' : 's'} · Prices and availability are rechecked when you sign in and place your order.</p>
        </div>
        {!canCheckout && <p className="mt-4 text-sm text-red-600">Remove unavailable items{restaurantIds.size > 1 ? ' and choose items from one restaurant' : ''} before checkout.</p>}
        <button disabled={!canCheckout} onClick={() => navigate('/checkout')}
          className="w-full bg-primary text-white py-3 rounded-lg font-semibold hover:bg-primary/90 transition mt-5 disabled:bg-gray-300 disabled:text-gray-600 disabled:cursor-not-allowed">
          {localStorage.getItem('sessionToken') ? 'Proceed to Checkout' : 'Sign in to Checkout'}
        </button>
      </section>
    </div>
  </div>;
}

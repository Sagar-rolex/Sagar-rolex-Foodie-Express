import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { cartApi } from '../api/cartApi';
import { orderApi } from '../api/orderApi';
import apiClient from '../api/axios';
import { Cart, OrderRequest } from '../types';
import { toast } from 'sonner';
import { calculateCartTotals, formatCurrency } from '../lib/utils';
import { CreditCard, ShieldCheck } from 'lucide-react';
import SafeImage from '../components/SafeImage';
import CouponPanel from '../components/CouponPanel';
import type { CouponTotals } from '../api/couponApi';

interface CartData { cart: Cart; total: number }

declare global {
  interface Window {
    Razorpay: any;
  }
}

export default function Checkout() {
  const [cartData, setCartData] = useState<CartData | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [couponTotals, setCouponTotals] = useState<CouponTotals|null>(null);
  const submissionLock = useRef(false);
  const [formData, setFormData] = useState({
    fullName: '',
    phone: '',
    address: '',
    city: '',
    pincode: '',
    instructions: '',
  });
  const navigate = useNavigate();

  const fetchCart = useCallback(async () => {
    try {
      const response = await cartApi.getCart();
      setCartData(response.data);
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Please login to checkout');
        navigate('/login');
      } else {
        toast.error('Failed to load cart');
      }
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    fetchCart();
    // Load Razorpay script dynamically
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    document.body.appendChild(script);
  }, [fetchCart]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!cartData || submissionLock.current) return;

    const unavailable = cartData.cart.items.some(item => !item.foodItem.available || !item.foodItem.restaurant?.active);
    const restaurantIds = new Set(cartData.cart.items.map(item => item.foodItem.restaurant?.id));
    if (unavailable || restaurantIds.size !== 1) {
      toast.error(unavailable ? 'Remove unavailable dishes from your cart before checkout.' : 'Please order items from one restaurant at a time.');
      await fetchCart();
      return;
    }

    if (!formData.fullName || !formData.phone || !formData.address || !formData.city || !formData.pincode) {
      toast.error('Please fill in all delivery details');
      return;
    }

    submissionLock.current = true;
    setSubmitting(true);
    try {
      const displayedSubtotal = calculateCartTotals(cartData.cart.items).subtotal;
      const deliveryAddress = `${formData.fullName}, ${formData.address}, ${formData.city} - ${formData.pincode}`;
      const selectedCoupon = couponTotals?.code || sessionStorage.getItem('foodieCoupon') || undefined;
      const orderRequest: OrderRequest = { deliveryAddress, couponCode: selectedCoupon };

      // Create a pending server-side order. The cart stays intact until payment verifies.
      const orderResponse = await orderApi.placeOrder(orderRequest);

      if (!orderResponse.success || !orderResponse.data) {
        throw new Error('Failed to create order');
      }

      const localOrder = orderResponse.data;
      const orderId = localOrder.id;

      if (Math.round(Number(localOrder.totalAmount) * 100) !== Math.round(displayedSubtotal * 100)) {
        await fetchCart();
        toast.warning('A dish price changed. We updated your cart; review the new total and submit again to confirm.');
        submissionLock.current = false;
        setSubmitting(false);
        return;
      }

      // Razorpay order amount is calculated from the saved bill on the server.
      const paymentOrderRes = await apiClient.post('/payments/create-order', { orderId });
      const paymentData = paymentOrderRes.data.data;

      const { keyId, razorpayOrderId, amount, currency } = paymentData;

      // Step 3: Open Razorpay Checkout modal
      const options = {
        key: keyId,
        amount: Number(amount),
        currency: currency || 'INR',
        name: 'Foodie Express',
        description: `Order #${orderId} Payment`,
        order_id: razorpayOrderId,
        handler: async function (response: any) {
          try {
            // Step 4: Verify payment signature on backend
            const verifyRes = await apiClient.post('/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });

            if (verifyRes.data.success && verifyRes.data.data?.status === 'SUCCESS') {
              sessionStorage.removeItem('foodieCoupon');
              window.dispatchEvent(new Event('cart:updated'));
              toast.success('Payment verified successfully!');
              navigate(`/order-confirmation/${orderId}`);
            } else {
              toast.error('Payment is not verified. Your cart is saved; retry checkout.');
              submissionLock.current = false;
              setSubmitting(false);
            }
          } catch (verifyErr: any) {
            toast.error(verifyErr.response?.data?.message || 'Payment verification failed');
            submissionLock.current = false;
            setSubmitting(false);
          }
        },
        prefill: {
          name: formData.fullName,
          contact: formData.phone,
        },
        theme: {
          color: '#f97316',
        },
        modal: {
          ondismiss: function() {
            submissionLock.current = false;
            setSubmitting(false);
            toast.info('Payment was not completed. Your cart is saved so you can retry.');
          }
        }
      };

      if (!window.Razorpay) throw new Error('Razorpay Checkout did not load. Check your connection and try again.');
      const rzp1 = new window.Razorpay(options);
      rzp1.on('payment.failed', (response: any) => {
        void apiClient.post('/payments/failed', { razorpayPaymentId: response.error?.metadata?.payment_id }).catch(() => undefined);
        submissionLock.current = false;
        setSubmitting(false);
        toast.error(response.error?.description || 'Payment failed. Your cart is saved; try again.');
      });
      rzp1.open();

    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to initialize payment');
      submissionLock.current = false;
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
        <div className="bg-gray-200 h-96 rounded-lg animate-pulse" />
      </div>
    );
  }

  if (!cartData || cartData.cart.items.length === 0) {
    return <Navigate to="/cart" replace />;
  }

  const { cart } = cartData;
  const baseTotals = calculateCartTotals(cart.items);
  const { subtotal, tax, deliveryFee, grandTotal } = couponTotals || baseTotals;
  const unavailableItems = cart.items.filter(item => !item.foodItem.available || !item.foodItem.restaurant?.active);
  const restaurantIds = new Set(cart.items.map(item => item.foodItem.restaurant?.id));
  const canCheckout = unavailableItems.length === 0 && restaurantIds.size === 1;
  const hasChangedPrices = cart.items.some(item => item.priceAtAdd != null
    && Math.round(Number(item.priceAtAdd) * 100) !== Math.round(Number(item.foodItem.price) * 100));

  const itemsByRestaurant = cart.items.reduce((acc, item) => {
    const restaurantId = item.foodItem.restaurant.id;
    if (!acc[restaurantId]) {
      acc[restaurantId] = {
        restaurant: item.foodItem.restaurant,
        items: [],
      };
    }
    acc[restaurantId].items.push(item);
    return acc;
  }, {} as { [key: number]: { restaurant: { id: number; name: string }; items: typeof cart.items } });

  return (
    <div className="min-h-[80vh] bg-gray-50 py-8">
      <div className="max-w-6xl mx-auto px-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Delivery Address Form */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-6 flex items-center space-x-2">
              <span>📍</span>
              <span>Delivery Details</span>
            </h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                  placeholder="Your full name"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone Number</label>
                <input
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                  placeholder="Your 10-digit phone number"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Street Address</label>
                <input
                  name="address"
                  value={formData.address}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                  placeholder="Street address, building, apartment"
                  required
                />
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                  <input
                    name="city"
                    value={formData.city}
                    onChange={handleChange}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                    placeholder="City"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Pincode</label>
                  <input
                    name="pincode"
                    value={formData.pincode}
                    onChange={handleChange}
                    className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                    placeholder="Pincode"
                    required
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Delivery Instructions (Optional)</label>
                <textarea
                  name="instructions"
                  value={formData.instructions}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                  placeholder="Any special instructions for delivery"
                  rows={3}
                />
              </div>

              <div className="pt-4">
                {!canCheckout && <p className="text-sm text-red-600 mb-3">Remove unavailable dishes and make sure all items are from one restaurant.</p>}
                {hasChangedPrices && <p role="status" className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded-lg p-3 mb-3">Some dish prices changed since you added them. Review the updated amounts before continuing.</p>}
                <button
                  type="submit"
                  disabled={submitting || !canCheckout}
                  className="w-full bg-orange-600 text-white py-3.5 px-6 rounded-xl font-semibold hover:bg-orange-700 active:scale-[0.99] transition duration-150 flex items-center justify-center space-x-2 shadow-lg shadow-orange-950/20 disabled:opacity-60"
                >
                  {submitting ? (
                    <>
                      <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Initializing Secure Checkout...</span>
                    </>
                  ) : (
                    <>
                      <CreditCard size={20} />
                      <span>Proceed to Razorpay Payment ({formatCurrency(grandTotal)})</span>
                    </>
                  )}
                </button>
                <div className="flex items-center justify-center space-x-1.5 text-xs text-gray-400 mt-3">
                  <ShieldCheck size={14} className="text-emerald-600" />
                  <span>256-bit SSL Secure Payment via Razorpay Test Mode</span>
                </div>
              </div>
            </form>
          </div>

          {/* Order Summary */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8 h-fit">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900">Order Summary</h2>
              <span className="bg-orange-50 text-orange-700 px-3 py-1 rounded-full text-xs font-semibold">
                {cart.items.reduce((sum, item) => sum + item.quantity, 0)} items
              </span>
            </div>

            {Object.values(itemsByRestaurant).map(({ restaurant, items }) => (
              <div key={restaurant.id} className="mb-5">
                <div className="flex justify-between items-center mb-3">
                  <h3 className="font-semibold text-gray-900 text-sm">
                    🏪 {restaurant.name}
                  </h3>
                </div>
                <div className="space-y-3">
                  {items.map((item) => (
                    <div key={item.id} className="flex items-center justify-between gap-3 text-sm pb-2 border-b border-gray-50 last:border-b-0">
                      <div className="flex min-w-0 flex-1 items-center gap-3 pr-2">
                        <SafeImage imagePath={item.foodItem.imagePath} alt={item.foodItem.name} className="h-12 w-12 shrink-0 rounded-lg" />
                        <div className="min-w-0">
                          <p className="truncate font-medium text-gray-800">{item.foodItem.name}</p>
                          <p className="text-xs text-gray-400">Qty: {item.quantity}</p>
                        </div>
                      </div>
                      <div className="font-semibold text-gray-900">
                        {formatCurrency(item.foodItem.price * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}

            <hr className="my-6 border-gray-100" />

            <CouponPanel onApplied={setCouponTotals} />

            <div className="space-y-3 text-sm text-gray-600">
              <div className="flex justify-between">
                <span>Subtotal</span>
                <span className="font-medium text-gray-900">{formatCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span>Taxes & Charges (10%)</span>
                <span className="font-medium text-gray-900">{formatCurrency(tax)}</span>
              </div>
              <div className="flex justify-between">
                <span>Delivery Fee</span>
                <span className="font-medium text-gray-900">{formatCurrency(deliveryFee)}</span>
              </div>
              {couponTotals&&<div className="flex justify-between text-green-700"><span>Discount ({couponTotals.code})</span><span>−{formatCurrency(couponTotals.discount)}</span></div>}
              <hr className="my-3 border-gray-100" />
              <div className="flex justify-between text-lg font-bold text-gray-900">
                <span>Total Amount</span>
                <span className="text-orange-600">{formatCurrency(grandTotal)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

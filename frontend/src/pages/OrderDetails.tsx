import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate, Navigate } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { billApi } from '../api/billApi';
import type { Order, Bill } from '../types';
import { toast } from 'sonner';
import { formatDate, formatCurrency, getOrderStatusColor } from '../lib/utils';
import { ArrowLeft, MapPin, CreditCard } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function OrderDetails() {
  const { id } = useParams<{ id: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [bill, setBill] = useState<Bill | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchOrderDetails = async () => {
      if (!id) return;
      try {
        const orderResponse = await orderApi.getOrderById(Number(id));
        setOrder(orderResponse.data);

        try {
          const billResponse = await billApi.getBillByOrder(Number(id));
          setBill(billResponse.data);
        } catch {
          // Bill might not exist yet
        }
      } catch (error: any) {
        if (error.response?.status === 401) {
          toast.error('Please login to view order');
          navigate('/login');
        } else {
          toast.error('Failed to load order details');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchOrderDetails();
  }, [id, navigate]);

  const handleCancelOrder = async () => {
    if (!order) return;
    if (!window.confirm('Are you sure you want to cancel this order?')) return;

    setCancelling(true);
    try {
      const response = await orderApi.cancelOrder(order.id);
      setOrder(response.data);
      toast.success('Order cancelled successfully');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to cancel order');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="bg-gray-200 h-64 rounded-lg animate-pulse" />
      </div>
    );
  }

  if (!order) {
    return <Navigate to="/orders" replace />;
  }

  const canCancel = order.status === 'PLACED';

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <Link to="/orders" className="flex items-center text-gray-600 hover:text-gray-900 mb-6">
        <ArrowLeft size={20} className="mr-2" />
        Back to Orders
      </Link>

      {/* Order Header */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Order #{order.id}</h1>
            <p className="text-gray-600 mt-1">Placed on {formatDate(order.orderTime)}</p>
          </div>
          <div className="mt-4 md:mt-0">
            <span className={`px-4 py-2 rounded-full text-lg font-medium ${getOrderStatusColor(order.status)}`}>
              {order.status.replace(/_/g, ' ')}
            </span>
          </div>
        </div>
      </div>

      {/* Order Status Timeline */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Order Status</h2>
        <div className="flex flex-wrap gap-2">
          {['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'].map((status) => {
            const statusIndex = ['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].indexOf(order.status);
            const currentIndex = ['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'].indexOf(status);
            const isCompleted = currentIndex <= statusIndex;
            const isCurrent = order.status === status;

            return (
              <div
                key={status}
                className={`flex items-center ${isCompleted ? 'text-green-600' : 'text-gray-400'}`}
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isCompleted ? 'bg-green-100' : 'bg-gray-200'}`}>
                  {isCompleted ? '✓' : (currentIndex + 1)}
                </div>
                <span className={`ml-2 font-medium ${isCurrent ? 'text-blue-600' : ''}`}>
                  {status.replace(/_/g, ' ')}
                </span>
                {currentIndex < 4 && <span className="mx-2 text-gray-400">→</span>}
              </div>
            );
          })}
        </div>
      </div>

      {/* Restaurant Info */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Restaurant</h2>
        <SafeImage imagePath={order.restaurant.imagePath} alt={`${order.restaurant.name} restaurant`} kind="restaurant" className="mb-4 h-48 w-full rounded-lg" />
        <p className="text-lg font-semibold">{order.restaurant.name}</p>
        <p className="text-gray-600">{order.restaurant.address}</p>
      </div>

      {/* Order Items */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-bold mb-4">Order Items</h2>
        <div className="space-y-3">
          {order.items.map((item) => (
            <div key={item.id} className="flex justify-between items-center">
              <div className="flex items-center space-x-3">
                <SafeImage imagePath={item.foodItem.imagePath} alt={item.foodItem.name} className="h-14 w-14 shrink-0 rounded-lg" />
                <div>
                  <p className="font-medium">{item.itemNameAtOrder || item.foodItem.name}</p>
                  <p className="text-gray-500 text-sm">Qty: {item.quantity}</p>
                </div>
              </div>
              <span className="font-semibold">{formatCurrency(item.priceAtOrder * item.quantity)}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Delivery Address */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-bold mb-4 flex items-center">
          <MapPin className="mr-2" />
          Delivery Address
        </h2>
        <p className="text-gray-700">{order.deliveryAddress}</p>
      </div>

      {/* Bill Summary */}
      {bill && (
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <h2 className="text-xl font-bold mb-4 flex items-center">
            <CreditCard className="mr-2" />
            Bill Summary
          </h2>
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-600">Subtotal</span>
              <span>{formatCurrency(bill.totalAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Tax</span>
              <span>{formatCurrency(bill.tax)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">Delivery</span>
              <span>{formatCurrency(bill.deliveryFee || 0)}</span>
            </div>
            {bill.couponCode && <div className="flex justify-between text-green-700"><span>Coupon ({bill.couponCode})</span><span>−{formatCurrency(bill.discount || 0)}</span></div>}
            <div className="flex justify-between text-sm text-gray-600">
              <span>Payment status</span>
              <span>{bill.paymentStatus}</span>
            </div>
            <hr className="my-2" />
            <div className="flex justify-between text-lg font-bold">
              <span>Total</span>
              <span className="text-blue-600">{formatCurrency(bill.grandTotal)}</span>
            </div>
            <div className="mt-4">
              <Link
                to={`/bills/${bill.id}`}
                className="inline-block bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition"
              >
                View Invoice
              </Link>
            </div>
          </div>
        </div>
      )}

      {/* Actions */}
      {canCancel && (
        <div className="flex justify-end">
          <button
            onClick={handleCancelOrder}
            disabled={cancelling}
            className="bg-red-500 text-white px-6 py-2 rounded-lg hover:bg-red-600 transition disabled:opacity-50"
          >
            {cancelling ? 'Cancelling...' : 'Cancel Order'}
          </button>
        </div>
      )}
    </div>
  );
}

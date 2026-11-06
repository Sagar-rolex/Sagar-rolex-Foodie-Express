import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { Order } from '../types';
import { toast } from 'sonner';
import { CheckCircle, Package, Clock } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function OrderConfirmation() {
  const { orderId } = useParams<{ orderId: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchOrder = async () => {
      if (!orderId) return;
      try {
        const response = await orderApi.getOrderById(Number(orderId));
        setOrder(response.data);
      } catch (error) {
        toast.error('Failed to load order details');
        navigate('/');
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [orderId, navigate]);

  if (loading) {
    return (
      <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
        <div className="animate-pulse">
          <div className="h-12 w-12 bg-gray-200 rounded-full mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Processing your order...</h2>
          <p className="text-gray-600">Please wait while we prepare your confirmation</p>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
        <div className="text-center">
          <Package size={48} className="mx-auto text-gray-400 mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Order not found</h2>
          <p className="text-gray-600 mb-6">We couldn't find your order details.</p>
          <div className="flex space-x-3">
            <button
              onClick={() => navigate('/')}
              className="bg-gray-200 text-gray-800 px-4 py-2 rounded-lg hover:bg-gray-300 transition"
            >
              Go Home
            </button>
            <button
              onClick={() => navigate('/restaurants')}
              className="bg-primary text-white px-4 py-2 rounded-lg font-semibold hover:bg-primary/90 transition"
            >
              Browse Restaurants
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Calculate estimated delivery time (30-40 minutes from order time)
  const estimatedDelivery = new Date(
    new Date(order.orderTime).getTime() + 35 * 60 * 1000
  ); // 35 minutes average

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
      <div className="text-center space-y-6">
        {/* Success Icon */}
        <div className="flex items-center justify-center w-20 h-20 bg-primary/10 rounded-full mb-6">
          <CheckCircle size={32} className="text-primary" />
        </div>

        {/* Title */}
        <h1 className="text-3xl font-bold text-gray-900">
          Order placed!
        </h1>

        {/* Order Number */}
        <div className="bg-primary/10 text-primary px-4 py-2 rounded-lg inline-block text-lg font-medium">
          Order #FE{order.id.toString().padStart(4, '0')}
        </div>

        {/* Message */}
        <p className="text-gray-600 max-w-md">
          Your delicious food is being prepared.
        </p>

        {/* Delivery Time */}
        <div className="flex items-center space-x-3 text-gray-600 mt-4">
          <Clock size={20} className="text-gray-400" />
          <div className="flex-1">
            <p className="text-sm">Estimated delivery</p>
            <p className="text-lg font-medium text-gray-900">
              {formatTime(estimatedDelivery)}
            </p>
          </div>
        </div>

        {order.items[0] && (
          <div className="mx-auto flex max-w-sm items-center gap-4 rounded-2xl border border-orange-100 bg-white p-3 text-left shadow-sm">
            <SafeImage imagePath={order.items[0].foodItem.imagePath} alt={order.items[0].foodItem.name} className="h-20 w-20 shrink-0 rounded-xl" />
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-orange-600">Your order</p>
              <p className="font-bold text-gray-900">{order.items[0].foodItem.name}</p>
              {order.items.length > 1 && <p className="text-sm text-gray-500">and {order.items.length - 1} more item(s)</p>}
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex space-x-3 mt-8">
          <button
            onClick={() => navigate(`/orders/${order.id}`)}
            className="flex-1 bg-gray-200 text-gray-800 px-6 py-3 rounded-lg font-medium hover:bg-gray-300 transition"
          >
            Track Order
          </button>
          <button
            onClick={() => navigate('/orders')}
            className="flex-1 bg-primary text-white px-6 py-3 rounded-lg font-semibold hover:bg-primary/90 transition"
          >
            View Orders
          </button>
        </div>

        {/* Footer Note */}
        <p className="text-xs text-gray-500 mt-8">
          You'll receive updates on your order status via notification.
        </p>
      </div>
    </div>
  );
}

import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { billApi } from '../api/billApi';
import { Order } from '../types';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { formatDate, getOrderStatusColor } from '../lib/utils';
import { Package, Clock } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [paymentStatuses, setPaymentStatuses] = useState<Record<number, string>>({});
  const [billTotals, setBillTotals] = useState<Record<number, number>>({});
  const navigate = useNavigate();

  const fetchOrders = useCallback(async () => {
    try {
      const response = await orderApi.getMyOrders();
      setOrders(response.data);
      const billResults = await Promise.allSettled(response.data.map(order => billApi.getBillByOrder(order.id)));
      setPaymentStatuses(Object.fromEntries(billResults.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[response.data[index].id, result.value.data.paymentStatus]] : []
      )));
      setBillTotals(Object.fromEntries(billResults.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[response.data[index].id, result.value.data.grandTotal]] : []
      )));
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Please login to view orders');
        navigate('/login');
      } else {
        toast.error('Failed to load orders');
      }
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-gray-200 rounded-lg h-96 animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">My Orders</h1>
        <p className="text-sm text-gray-500">
          {orders.length} {orders.length === 1 ? 'order' : 'orders'}
        </p>
      </div>

      {orders.length === 0 ? (
        <div className="text-center py-12">
          <Package size={48} className="mx-auto text-gray-400 mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">No orders yet</h2>
          <p className="text-gray-600 mb-6">Order some delicious food to get started!</p>
          <Link
            to="/restaurants"
            className="inline-block bg-primary text-white px-6 py-3 rounded-lg font-semibold hover:bg-primary/90 transition"
          >
            Browse Restaurants
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {orders.map((order) => (
            <Link key={order.id} to={`/orders/${order.id}`}>
              <div className="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-transform hover:scale-[1.02]">
                {/* Restaurant Info */}
                <SafeImage imagePath={order.items[0]?.foodItem.imagePath} alt={order.items[0]?.foodItem.name || `${order.restaurant.name} meal`} className="h-40 w-full" />
                <div className="flex flex-col md:flex-row items-start gap-4 p-6">
                  <div className="flex-1">
                    <div className="flex items-center mb-2">
                      <h3 className="text-lg font-bold text-gray-900">Order #{order.id}</h3>
                      <span className={`px-3 py-1 rounded-full text-sm font-medium ${getOrderStatusColor(order.status)}`}>
                        {order.status.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <p className="text-gray-600">🏪 {order.restaurant.name}</p>
                    <p className="text-gray-500 text-sm">
                      {order.items.length} item(s) • {formatDate(order.orderTime)}
                    </p>
                    <p className="mt-1 text-sm text-gray-600">Payment: {paymentStatuses[order.id] || 'PENDING'}</p>
                  </div>

                  {/* Price & Status */}
                  <div className="flex items-center space-x-4 text-right">
                    <div className="flex-1 text-right">
                      <span className="text-xl font-bold text-blue-600">₹{billTotals[order.id] ?? order.totalAmount}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Clock size={20} className="text-gray-400" />
                    </div>
                  </div>
                </div>

                {/* Status Progress Indicator for Active Orders */}
                {!['DELIVERED', 'CANCELLED'].includes(order.status) && (
                  <div className="mt-4 w-full">
                    <div className="flex items-center space-x-3 mb-2">
                      <span className="text-sm font-medium text-gray-600">Order Status</span>
                    </div>
                    <div className="flex space-x-2">
                      {['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'].map((status, index) => {
                        const statusIndex = ['PLACED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].indexOf(order.status);
                        const isCompleted = index <= statusIndex;
                        const isCurrent = order.status === status;

                        return (
                          <div key={status} className="flex items-center space-x-2">
                            <div className={`w-3 h-3 rounded-full flex items-center justify-center ${isCompleted ? 'bg-green-500' : isCurrent ? 'bg-primary' : 'bg-gray-200'}`} />
                            {index < 4 && <span className="w-0.5 h-0.5 bg-gray-200"></span>}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {/* Action Buttons for Delivered Orders */}
                {order.status === 'DELIVERED' && (
                  <div className="mt-4 flex space-x-3">
                    <button
                      onClick={() => navigate(`/bills/${order.id}`)}
                      className="flex-1 bg-gray-200 text-gray-800 px-4 py-2 rounded-lg font-medium hover:bg-gray-300 transition"
                    >
                      Download Bill
                    </button>
                    <button
                      onClick={() => navigate(`/orders/${order.id}`)}
                      className="flex-1 bg-primary text-white px-4 py-2 rounded-lg font-semibold hover:bg-primary/90 transition"
                    >
                      View Details
                    </button>
                  </div>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { orderApi } from '../api/orderApi';
import { billApi } from '../api/billApi';
import { Order } from '../types';
import { toast } from 'sonner';
import { formatDate, formatCurrency, getOrderStatusColor } from '../lib/utils';
import { Package } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [paymentStatuses, setPaymentStatuses] = useState<Record<number, string>>({});
  const [billTotals, setBillTotals] = useState<Record<number, number>>({});
  const [reordering, setReordering] = useState<number | null>(null);
  const navigate = useNavigate();

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      // Use the simple customer order-history endpoint. Payment verification saves
      // the order before checkout redirects here, so no tracking endpoint is needed.
      const response = await orderApi.getMyOrders();
      const customerOrders = Array.isArray(response.data) ? response.data : [];
      setOrders(customerOrders);

      const billResults = await Promise.allSettled(customerOrders.map(order => billApi.getBillByOrder(order.id)));
      setPaymentStatuses(Object.fromEntries(billResults.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[customerOrders[index].id, result.value.data.paymentStatus]] : []
      )));
      setBillTotals(Object.fromEntries(billResults.flatMap((result, index) =>
        result.status === 'fulfilled' ? [[customerOrders[index].id, result.value.data.grandTotal]] : []
      )));
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Please login to view orders');
        navigate('/login');
      } else {
        const message = error.response?.data?.message
          || (error.request ? 'Could not connect to the backend. Make sure the Spring Boot server is running.' : 'Failed to load orders');
        toast.error(message);
      }
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    void fetchOrders();
  }, [fetchOrders]);

  const handleReorder = async (orderId: number) => {
    setReordering(orderId);
    try {
      const { data } = await orderApi.reorder(orderId);
      if (data.addedItems.length) toast.success(`Added ${data.addedItems.length} item(s) to your cart.`);
      if (data.unavailableItems.length) toast.warning(`Unavailable or skipped: ${data.unavailableItems.join(', ')}`);
      if (data.addedItems.length) navigate('/cart');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Could not reorder these items.');
    } finally {
      setReordering(null);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => <div key={i} className="h-72 animate-pulse rounded-lg bg-gray-200" />)}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">My Orders</h1>
        <p className="text-sm text-gray-500">{orders.length} {orders.length === 1 ? 'order' : 'orders'}</p>
      </div>

      {orders.length === 0 ? (
        <div className="py-12 text-center">
          <Package size={48} className="mx-auto mb-4 text-gray-400" />
          <h2 className="mb-2 text-2xl font-bold text-gray-900">No orders yet</h2>
          <p className="mb-6 text-gray-600">Paid orders will appear here after payment confirmation.</p>
          <Link to="/restaurants" className="inline-block rounded-lg bg-primary px-6 py-3 font-semibold text-white transition hover:bg-primary/90">
            Browse Restaurants
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          {orders.map(order => (
            <article key={order.id} className="overflow-hidden rounded-xl bg-white shadow-lg">
              <Link to={`/orders/${order.id}`} className="block">
                <SafeImage imagePath={order.items[0]?.foodItem.imagePath} alt={order.items[0]?.foodItem.name || `${order.restaurant.name} meal`} className="h-40 w-full" />
                <div className="space-y-3 p-5">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <h2 className="text-lg font-bold text-gray-900">Order #{order.id}</h2>
                    <span className={`rounded-full px-3 py-1 text-sm font-medium ${getOrderStatusColor(order.status)}`}>
                      {order.status.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className="text-gray-700">{order.restaurant.name}</p>
                  <p className="text-sm text-gray-500">{order.items.length} item(s) · {formatDate(order.orderTime)}</p>
                  <div className="flex justify-between border-t pt-3 text-sm">
                    <span className="text-gray-600">Payment: {paymentStatuses[order.id] || 'PENDING'}</span>
                    <span className="font-bold text-orange-600">{formatCurrency(billTotals[order.id] ?? order.totalAmount)}</span>
                  </div>
                </div>
              </Link>
              <div className="flex gap-2 px-5 pb-5">
                <button onClick={() => navigate(`/orders/${order.id}`)} className="min-h-11 flex-1 rounded-lg border px-3 py-2 text-sm">View order</button>
                {paymentStatuses[order.id] === 'PAID' && <button onClick={() => navigate(`/bills/${order.id}`)} className="min-h-11 flex-1 rounded-lg bg-gray-100 px-3 py-2 text-sm">Invoice</button>}
                <button disabled={reordering === order.id} onClick={() => handleReorder(order.id)} className="min-h-11 flex-1 rounded-lg bg-orange-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50">
                  {reordering === order.id ? 'Adding…' : 'Reorder'}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

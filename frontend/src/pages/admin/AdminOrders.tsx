import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/adminApi';
import type { OrderStatus, Restaurant } from '../../types';
import type { AdminOrder } from '../../types/admin';
import { toast } from 'sonner';
import { formatCurrency, getOrderStatusColor } from '../../lib/utils';
import { Package } from 'lucide-react';

const transitions: Record<OrderStatus, OrderStatus[]> = {
  PLACED: ['CANCELLED'], CONFIRMED: ['PREPARING'], PREPARING: ['OUT_FOR_DELIVERY'],
  OUT_FOR_DELIVERY: ['DELIVERED'], DELIVERED: [], CANCELLED: [],
};

export default function AdminOrders() {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [paymentStatus, setPaymentStatus] = useState('');
  const [restaurantId, setRestaurantId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [updating, setUpdating] = useState<number | null>(null);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const response = await adminApi.getAllOrders({ search: search || undefined, status: status || undefined,
        paymentStatus: paymentStatus || undefined, restaurantId: restaurantId ? Number(restaurantId) : null,
        from: from || undefined, to: to || undefined, page, size: 20 });
      setOrders(response.data.content);
      setTotalPages(response.data.totalPages);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to load orders');
    } finally { setLoading(false); }
  }, [search, status, paymentStatus, restaurantId, from, to, page]);

  useEffect(() => { adminApi.getRestaurants().then(r => setRestaurants(r.data)).catch(() => toast.error('Failed to load restaurants')); }, []);
  useEffect(() => { void fetchOrders(); }, [fetchOrders]);

  const handleStatusUpdate = async (orderId: number, newStatus: OrderStatus) => {
    setUpdating(orderId);
    try { await adminApi.updateOrderStatus(orderId, newStatus); toast.success('Order fulfilment status updated'); await fetchOrders(); }
    catch (error: any) { toast.error(error.response?.data?.message || 'Status transition was rejected'); }
    finally { setUpdating(null); }
  };

  const clearFilters = () => { setSearch(''); setStatus(''); setPaymentStatus(''); setRestaurantId(''); setFrom(''); setTo(''); setPage(0); };

  return (
    <div className="min-h-screen bg-gray-100 lg:ml-64">
      <aside className="fixed left-0 top-0 hidden h-full w-64 bg-gray-900 p-4 text-white lg:block">
        <h1 className="mb-8 text-xl font-bold">Foodie Express <span className="block text-sm font-normal text-gray-400">Admin Panel</span></h1>
        <nav className="space-y-2">
          <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/dashboard">Dashboard</Link>
          <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/restaurants">Restaurants</Link>
          <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/food-items">Food Items</Link>
          <Link className="block rounded-lg bg-gray-800 px-4 py-3 text-white" to="/admin/orders">Orders</Link>
          <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/customers">Customers</Link>
        </nav>
      </aside>

      <main className="p-4 sm:p-8">
        <div className="mb-6"><h1 className="text-3xl font-bold text-gray-900">Orders</h1><p className="text-gray-600">Track fulfilment separately from verified payment status.</p></div>
        <div className="mb-6 grid grid-cols-1 gap-3 rounded-xl bg-white p-4 shadow md:grid-cols-2 xl:grid-cols-4">
          <input aria-label="Search orders" value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} placeholder="Order ID, customer, email, restaurant" className="rounded-lg border px-3 py-2" />
          <select aria-label="Filter order status" value={status} onChange={e => { setStatus(e.target.value); setPage(0); }} className="rounded-lg border px-3 py-2"><option value="">All order statuses</option>{['PLACED','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED'].map(s => <option key={s}>{s}</option>)}</select>
          <select aria-label="Filter payment status" value={paymentStatus} onChange={e => { setPaymentStatus(e.target.value); setPage(0); }} className="rounded-lg border px-3 py-2"><option value="">All payment statuses</option>{['PENDING','PAID','FAILED','REFUNDED'].map(s => <option key={s}>{s}</option>)}</select>
          <select aria-label="Filter restaurant" value={restaurantId} onChange={e => { setRestaurantId(e.target.value); setPage(0); }} className="rounded-lg border px-3 py-2"><option value="">All restaurants</option>{restaurants.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}</select>
          <label className="text-sm text-gray-600">From<input aria-label="From date" type="date" value={from} onChange={e => { setFrom(e.target.value); setPage(0); }} className="mt-1 block w-full rounded-lg border px-3 py-2" /></label>
          <label className="text-sm text-gray-600">To<input aria-label="To date" type="date" value={to} onChange={e => { setTo(e.target.value); setPage(0); }} className="mt-1 block w-full rounded-lg border px-3 py-2" /></label>
          <button onClick={clearFilters} className="self-end rounded-lg bg-gray-100 px-4 py-2 text-gray-700 hover:bg-gray-200">Clear filters</button>
        </div>

        <div className="overflow-hidden rounded-xl bg-white shadow">
          {loading ? <div className="p-12 text-center text-gray-500">Loading orders…</div> : orders.length === 0 ? (
            <div className="p-16 text-center"><Package size={48} className="mx-auto mb-3 text-gray-400"/><h2 className="font-bold text-gray-800">No matching orders</h2><p className="text-gray-500">Try changing the filters.</p></div>
          ) : <><div className="hidden overflow-x-auto 2xl:block"><table className="w-full min-w-[1000px] text-left text-sm">
            <thead className="bg-gray-50 text-xs uppercase text-gray-500"><tr><th className="px-4 py-3">Order</th><th className="px-4 py-3">Customer</th><th className="px-4 py-3">Items</th><th className="px-4 py-3">Total</th><th className="px-4 py-3">Payment</th><th className="px-4 py-3">Fulfilment</th></tr></thead>
            <tbody className="divide-y">
              {orders.map(order => <tr key={order.id} className="align-top hover:bg-gray-50">
                <td className="px-4 py-4"><p className="font-bold">#{order.id}</p><p className="text-xs text-gray-500">{new Date(order.orderTime).toLocaleString()}</p><p className="text-xs text-gray-500">{order.restaurantName}</p></td>
                <td className="px-4 py-4"><p className="font-medium">{order.customerName}</p><p className="text-gray-500">{order.customerEmail || '—'}</p><p className="text-gray-500">{order.customerPhone || '—'}</p><p className="mt-2 max-w-56 text-xs text-gray-600">Deliver to: {order.deliveryAddress || '—'}</p>{order.cancellationReason && <p className="mt-1 max-w-56 text-xs text-red-700">Reason: {order.cancellationReason}</p>}</td>
                <td className="px-4 py-4">{order.items.map((item,i) => <p key={`${item.name}-${i}`}>{item.name} × {item.quantity}</p>)}</td>
                <td className="px-4 py-4 font-semibold">{formatCurrency(order.totalAmount)} {order.currency}</td>
                <td className="px-4 py-4"><p className={`font-semibold ${order.paymentStatus === 'PAID' ? 'text-green-700' : 'text-amber-700'}`}>{order.paymentStatus}</p>{order.paymentMethod && <p>{order.paymentMethod}</p>}{order.paymentReference && <p className="max-w-40 truncate text-xs text-gray-500" title={order.paymentReference}>Ref: {order.paymentReference}</p>}</td>
                <td className="px-4 py-4"><span className={`mb-2 inline-block rounded-full px-3 py-1 text-xs font-medium ${getOrderStatusColor(order.status)}`}>{order.status.replace(/_/g,' ')}</span>{transitions[order.status].length > 0 && <select aria-label={`Update order ${order.id} status`} defaultValue="" disabled={updating === order.id} onChange={e => { if (e.target.value) void handleStatusUpdate(order.id, e.target.value as OrderStatus); }} className="block max-w-48 rounded-lg border px-2 py-1 disabled:opacity-50"><option value="">Update status…</option>{transitions[order.status].map(next => <option key={next} value={next}>{next.replace(/_/g,' ')}</option>)}</select>}</td>
              </tr>)}
            </tbody>
          </table></div>
          <div className="space-y-3 p-3 2xl:hidden">{orders.map(order => <article key={order.id} className="rounded-xl border border-gray-100 p-4 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-2"><div><p className="font-bold">Order #{order.id}</p><p className="text-xs text-gray-500">{new Date(order.orderTime).toLocaleString()}</p></div><span className={`rounded-full px-3 py-1 text-xs font-medium ${getOrderStatusColor(order.status)}`}>{order.status.replace(/_/g, ' ')}</span></div>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2"><div><p className="font-medium">{order.customerName}</p><p className="break-all text-sm text-gray-500">{order.customerEmail || '—'}</p><p className="text-sm text-gray-500">{order.customerPhone || '—'}</p></div><div><p className="text-sm font-semibold">{formatCurrency(order.totalAmount)} {order.currency}</p><p className="text-sm">Payment: {order.paymentStatus}{order.paymentMethod ? ` · ${order.paymentMethod}` : ''}</p>{order.paymentReference && <p className="break-all text-xs text-gray-500">Ref: {order.paymentReference}</p>}</div></div>
            <p className="mt-3 text-sm text-gray-600">{order.restaurantName} · Deliver to: {order.deliveryAddress}</p>
            <div className="mt-3 space-y-1 border-t pt-3 text-sm">{order.items.map((item, index) => <p key={`${item.name}-${index}`} className="flex justify-between gap-3"><span className="min-w-0 break-words">{item.name} × {item.quantity}</span><span className="shrink-0">{formatCurrency(item.unitPrice * item.quantity)}</span></p>)}</div>
            {order.cancellationReason && <p className="mt-2 text-sm text-red-700">Cancellation: {order.cancellationReason}</p>}
            {transitions[order.status].length > 0 && <label className="mt-3 block text-sm">Update status<select aria-label={`Update order ${order.id} status`} defaultValue="" disabled={updating === order.id} onChange={e => { if (e.target.value) void handleStatusUpdate(order.id, e.target.value as OrderStatus); }} className="mt-1 min-h-11 w-full rounded-lg border px-3 py-2 disabled:opacity-50"><option value="">Choose next status…</option>{transitions[order.status].map(next => <option key={next} value={next}>{next.replace(/_/g,' ')}</option>)}</select></label>}
          </article>)}</div></>}
        </div>
        <div className="mt-4 flex items-center justify-between"><span className="text-sm text-gray-500">Page {totalPages ? page + 1 : 0} of {totalPages}</span><div className="flex gap-2"><button disabled={page <= 0 || loading} onClick={() => setPage(p => p - 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Previous</button><button disabled={page + 1 >= totalPages || loading} onClick={() => setPage(p => p + 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Next</button></div></div>
      </main>
    </div>
  );
}

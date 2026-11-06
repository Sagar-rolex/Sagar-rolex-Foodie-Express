import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../../api/adminApi';
import type { AdminCustomer } from '../../types/admin';
import { toast } from 'sonner';
import { Users } from 'lucide-react';

export default function AdminCustomers() {
  const [customers, setCustomers] = useState<AdminCustomer[]>([]);
  const [search, setSearch] = useState('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState<number | null>(null);

  const loadCustomers = useCallback(async () => {
    setLoading(true);
    try {
      const result = await adminApi.getCustomers({ search: search || undefined, active: active === '' ? null : active === 'true', page, size: 20 });
      setCustomers(result.data.content);
      setTotalPages(result.data.totalPages);
    } catch (error: any) { toast.error(error.response?.data?.message || 'Failed to load customers'); }
    finally { setLoading(false); }
  }, [search, active, page]);

  useEffect(() => { void loadCustomers(); }, [loadCustomers]);

  const toggleActive = async (customer: AdminCustomer) => {
    const next = !customer.active;
    const verb = next ? 'reactivate' : 'deactivate';
    if (!window.confirm(`Are you sure you want to ${verb} ${customer.fullName}'s account?${next ? '' : ' They will be logged out and cannot sign in until reactivated.'}`)) return;
    setUpdating(customer.id);
    try {
      const response = await adminApi.setCustomerActive(customer.id, next);
      setCustomers(rows => rows.map(row => row.id === customer.id ? response.data : row));
      toast.success(`Customer account ${next ? 'activated' : 'deactivated'}`);
    } catch (error: any) { toast.error(error.response?.data?.message || 'Unable to change account status'); }
    finally { setUpdating(null); }
  };

  return <div className="min-h-screen bg-gray-100 lg:ml-64">
    <aside className="fixed left-0 top-0 hidden h-full w-64 bg-gray-900 p-4 text-white lg:block">
      <h1 className="mb-8 text-xl font-bold">Foodie Express <span className="block text-sm font-normal text-gray-400">Admin Panel</span></h1>
      <nav className="space-y-2">
        <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/dashboard">Dashboard</Link>
        <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/restaurants">Restaurants</Link>
        <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/food-items">Food Items</Link>
        <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/support">Support & FAQs</Link>
        <Link className="block rounded-lg px-4 py-3 text-gray-300 hover:bg-gray-800" to="/admin/orders">Orders</Link>
        <Link className="block rounded-lg bg-gray-800 px-4 py-3 text-white" to="/admin/customers">Customers</Link>
      </nav>
    </aside>
    <main className="p-4 sm:p-8">
      <div className="mb-6"><h1 className="text-3xl font-bold text-gray-900">Customers</h1><p className="text-gray-600">Account status and order history summary. Passwords and session tokens are never returned.</p></div>
      <div className="mb-6 grid grid-cols-1 gap-3 rounded-xl bg-white p-4 shadow sm:grid-cols-[1fr_220px]">
        <input aria-label="Search customers" value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} placeholder="Search name, username, or email" className="rounded-lg border px-3 py-2" />
        <select aria-label="Filter customer status" value={active} onChange={e => { setActive(e.target.value); setPage(0); }} className="rounded-lg border px-3 py-2"><option value="">All accounts</option><option value="true">Active</option><option value="false">Inactive</option></select>
      </div>
      <div className="overflow-hidden rounded-xl bg-white shadow">
        {loading ? <div className="p-12 text-center text-gray-500">Loading customers…</div> : customers.length === 0 ? <div className="p-16 text-center"><Users size={48} className="mx-auto mb-3 text-gray-400"/><h2 className="font-bold text-gray-800">No customers found</h2><p className="text-gray-500">Try a different search or filter.</p></div> :
          <div className="overflow-x-auto"><table className="w-full min-w-[800px] text-left text-sm"><thead className="bg-gray-50 text-xs uppercase text-gray-500"><tr><th className="px-4 py-3">Customer</th><th className="px-4 py-3">Contact</th><th className="px-4 py-3">Joined</th><th className="px-4 py-3">Orders</th><th className="px-4 py-3">Account</th><th className="px-4 py-3">Action</th></tr></thead>
            <tbody className="divide-y">{customers.map(customer => <tr key={customer.id} className="hover:bg-gray-50"><td className="px-4 py-4"><p className="font-semibold">{customer.fullName}</p><p className="text-gray-500">@{customer.username}</p></td><td className="px-4 py-4"><p>{customer.email || '—'}</p><p className="text-gray-500">{customer.phone || '—'}</p></td><td className="px-4 py-4">{customer.createdAt ? new Date(customer.createdAt).toLocaleDateString() : 'Date unavailable'}</td><td className="px-4 py-4">{customer.orderCount}</td><td className="px-4 py-4"><span className={`rounded-full px-3 py-1 text-xs font-semibold ${customer.active ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-700'}`}>{customer.active ? 'Active' : 'Inactive'}</span></td><td className="px-4 py-4"><button disabled={updating === customer.id} onClick={() => void toggleActive(customer)} className={`rounded-lg px-3 py-2 font-medium disabled:opacity-50 ${customer.active ? 'bg-red-50 text-red-700 hover:bg-red-100' : 'bg-green-50 text-green-700 hover:bg-green-100'}`}>{updating === customer.id ? 'Saving…' : customer.active ? 'Deactivate' : 'Reactivate'}</button></td></tr>)}</tbody>
          </table></div>}
      </div>
      <div className="mt-4 flex items-center justify-between"><span className="text-sm text-gray-500">Page {totalPages ? page + 1 : 0} of {totalPages}</span><div className="flex gap-2"><button disabled={page <= 0 || loading} onClick={() => setPage(p => p - 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Previous</button><button disabled={page + 1 >= totalPages || loading} onClick={() => setPage(p => p + 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Next</button></div></div>
    </main>
  </div>;
}

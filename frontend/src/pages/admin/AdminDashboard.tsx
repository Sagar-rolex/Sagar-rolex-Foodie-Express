import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { adminApi } from '../../api/adminApi';
import { authApi } from '../../api/authApi';
import type { AdminDashboardData } from '../../types/admin';
import { toast } from 'sonner';
import { LayoutDashboard, Store, Utensils, ShoppingCart, Users, LogOut, RefreshCw, TicketPercent, LifeBuoy } from 'lucide-react';
import { formatCurrency } from '../../lib/utils';

function TrendChart({ title, points, money = false }: { title: string; points: { date: string; value: number }[]; money?: boolean }) {
  const max = Math.max(1, ...points.map(point => Number(point.value)));
  return <section className="rounded-xl bg-white p-6 shadow-md" aria-label={title}>
    <h2 className="mb-5 text-lg font-bold text-gray-900">{title} <span className="text-sm font-normal text-gray-500">(last 7 days)</span></h2>
    {points.length === 0 ? <p className="py-8 text-center text-gray-500">No data for this period</p> : <div className="flex h-48 items-end gap-2" role="img" aria-label={`${title} bar chart`}>
      {points.map(point => <div key={point.date} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-2" title={`${point.date}: ${money ? formatCurrency(point.value) : point.value}`}>
        <span className="truncate text-[10px] text-gray-500">{money ? formatCurrency(point.value) : point.value}</span>
        <div className={`w-full rounded-t-md ${money ? 'bg-orange-500' : 'bg-blue-500'}`} style={{ height: `${Math.max(4, Number(point.value) / max * 72)}%` }} />
        <span className="text-[10px] text-gray-500">{point.date.slice(5)}</span>
      </div>)}
    </div>}
  </section>;
}

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<AdminDashboardData | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const response = await adminApi.getDashboard();
      if (!response.success || !response.data) {
        throw new Error(response.message || 'Failed to load dashboard data');
      }
      setDashboard(response.data);
    } catch (error: any) {
      if (error.response?.status === 401) return;
      setLoadError(
        error.response?.status === 403
          ? 'Your account does not have administrator access.'
          : error.response?.data?.message || error.message || 'Failed to load dashboard data.'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
      logout();
      navigate('/admin/login');
    } catch (error) {
      toast.error('Failed to logout');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-8">
        <div className="animate-pulse space-y-4">
          <div className="h-12 bg-gray-300 rounded w-1/4" />
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-300 rounded" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="fixed left-0 top-0 hidden h-full w-64 bg-gray-900 p-4 text-white lg:block">
        <div className="mb-8">
          <h1 className="text-xl font-bold flex items-center space-x-2">
            <span>🍽️</span>
            <span>Foodie Express</span>
          </h1>
          <p className="text-gray-400 text-sm mt-1">Admin Panel</p>
        </div>

        <nav className="space-y-2">
          <Link to="/admin/coupons" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"><TicketPercent size={20}/><span>Coupons & Offers</span></Link>
          <Link
            to="/admin/dashboard"
            className="flex items-center space-x-3 px-4 py-3 rounded-lg bg-gray-800 text-white"
          >
            <LayoutDashboard size={20} />
            <span>Dashboard</span>
          </Link>
          <Link to="/admin/customers" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"><Users size={20}/><span>Customers</span></Link>
          <Link to="/admin/support" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"><LifeBuoy size={20}/><span>Support & FAQs</span></Link>
          <Link
            to="/admin/restaurants"
            className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"
          >
            <Store size={20} />
            <span>Restaurants</span>
          </Link>
          <Link
            to="/admin/food-items"
            className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"
          >
            <Utensils size={20} />
            <span>Food Items</span>
          </Link>
          <Link
            to="/admin/orders"
            className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"
          >
            <ShoppingCart size={20} />
            <span>Orders</span>
          </Link>
        </nav>

        <div className="absolute bottom-4 left-4 right-4">
          <button
            onClick={handleLogout}
            className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition w-full"
          >
            <LogOut size={20} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="min-w-0 p-4 sm:p-6 lg:ml-64 lg:p-8">
        <div className="mb-8 flex flex-col items-start gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between">
          <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">Dashboard</h1>
          <p className="text-gray-600">Welcome back, {user?.username}</p>
          <button onClick={fetchDashboardData} className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-700 hover:bg-gray-50"><RefreshCw size={16}/>Refresh</button>
        </div>

        {loadError && (
          <div role="alert" className="mb-6 flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-red-800 sm:flex-row sm:items-center sm:justify-between">
            <p>{loadError}</p>
            <button type="button" onClick={fetchDashboardData} disabled={loading} className="rounded-lg bg-red-700 px-4 py-2 font-medium text-white hover:bg-red-800 disabled:opacity-60">
              {loading ? 'Retrying…' : 'Retry'}
            </button>
          </div>
        )}

        {dashboard?.unavailableMetrics.length ? (
          <div role="status" className="mb-6 rounded-lg border border-amber-200 bg-amber-50 p-4 text-amber-900">
            Some dashboard information could not be loaded: {dashboard.unavailableMetrics.join(', ')}.
          </div>
        ) : null}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 gap-6 mb-8 md:grid-cols-2 xl:grid-cols-3">
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Total Restaurants</p>
                <p className="text-3xl font-bold text-gray-900">{dashboard?.totalRestaurants ?? '—'}</p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                <Store className="text-blue-600" size={24} />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Total Orders</p>
                <p className="text-3xl font-bold text-gray-900">{dashboard?.totalOrders ?? '—'}</p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
                <ShoppingCart className="text-green-600" size={24} />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Total Revenue</p>
                <p className="text-3xl font-bold text-gray-900">₹{dashboard?.totalRevenue == null ? '—' : Number(dashboard.totalRevenue).toLocaleString()}</p>
              </div>
              <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center">
                <span className="text-purple-600 text-xl">💰</span>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-500 text-sm">Total Customers</p>
                <p className="text-3xl font-bold text-gray-900">{dashboard?.totalCustomers ?? '—'}</p>
              </div>
              <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center">
                <Users className="text-orange-600" size={24} />
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6"><p className="text-gray-500 text-sm">Available Dishes</p><p className="mt-2 text-3xl font-bold text-gray-900">{dashboard?.availableFoodItems ?? '—'}</p><Utensils className="mt-3 text-orange-600"/></div>
          <div className="bg-white rounded-lg shadow-md p-6"><p className="text-gray-500 text-sm">Awaiting Payment / Confirmation</p><p className="mt-2 text-3xl font-bold text-gray-900">{dashboard?.pendingOrders ?? '—'}</p><ShoppingCart className="mt-3 text-amber-600"/></div>
        </div>

        <div className="mb-8 grid grid-cols-1 gap-6 xl:grid-cols-2">
          <TrendChart title="Orders over time" points={dashboard?.ordersOverTime || []}/>
          <TrendChart title="Paid revenue over time" points={dashboard?.revenueOverTime || []} money/>
        </div>

        <div className="mb-8 rounded-xl bg-white p-6 shadow-md">
          <h2 className="mb-4 text-lg font-bold text-gray-900">Best-selling dishes</h2>
          {!dashboard?.bestSellingDishes.length ? <p className="py-6 text-center text-gray-500">Sales data appears after paid orders.</p> : <ol className="space-y-3">{dashboard.bestSellingDishes.map((dish, index) => <li key={dish.name} className="flex items-center justify-between border-b pb-2 last:border-0"><span><b className="mr-3 text-orange-600">{index + 1}.</b>{dish.name}</span><span className="font-semibold text-gray-700">{dish.quantity} sold</span></li>)}</ol>}
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Quick Actions</h2>
          <div className="flex flex-wrap gap-4">
            <Link
              to="/admin/restaurants"
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
            >
              Add Restaurant
            </Link>
            <Link
              to="/admin/food-items"
              className="px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
            >
              Add Food Item
            </Link>
            <Link
              to="/admin/orders"
              className="px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition"
            >
              View Orders
            </Link>
          </div>
        </div>

        {/* Recent Orders */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-bold text-gray-900 mb-4">Recent Orders</h2>
          {!dashboard?.recentOrders.length ? (
            <p className="text-gray-500 text-center py-8">{dashboard?.unavailableMetrics.includes('recentOrders') ? 'Recent orders could not be loaded.' : 'No orders yet'}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left border-b">
                    <th className="pb-3 font-semibold text-gray-600">Order ID</th>
                    <th className="pb-3 font-semibold text-gray-600">Restaurant</th>
                    <th className="pb-3 font-semibold text-gray-600">Amount</th>
                    <th className="pb-3 font-semibold text-gray-600">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.recentOrders.map((order) => (
                    <tr key={order.id} className="border-b last:border-b-0">
                      <td className="py-3">#{order.id}</td>
                      <td className="py-3">{order.restaurantName || 'Restaurant unavailable'}</td>
                      <td className="py-3">₹{order.totalAmount == null ? '—' : order.totalAmount}</td>
                      <td className="py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                          order.status === 'DELIVERED' ? 'bg-green-100 text-green-800' :
                          order.status === 'CANCELLED' ? 'bg-red-100 text-red-800' :
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {order.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

import { useState } from 'react';
import { Link, Outlet, Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Menu, X } from 'lucide-react';

const adminLinks = [
  ['Dashboard', '/admin/dashboard'],
  ['Restaurants', '/admin/restaurants'],
  ['Food Items', '/admin/food-items'],
  ['Orders', '/admin/orders'],
  ['Customers', '/admin/customers'],
  ['Coupons & Offers', '/admin/coupons'],
  ['Support & FAQs', '/admin/support'],
];

export default function AdminLayout() {
  const { isAuthenticated, isAdmin } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  // If not admin, redirect to home
  if (isAuthenticated && !isAdmin) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="flex flex-col min-h-screen bg-gray-100">
      {isAdmin && <header className="sticky top-0 z-50 border-b border-gray-700 bg-gray-900 text-white lg:hidden">
        <div className="flex min-h-14 items-center justify-between px-4">
          <Link to="/admin/dashboard" className="font-bold">Foodie Express · Admin</Link>
          <button type="button" aria-label={menuOpen ? 'Close admin navigation' : 'Open admin navigation'} aria-expanded={menuOpen} aria-controls="admin-mobile-navigation" onClick={() => setMenuOpen(open => !open)} className="grid h-11 w-11 place-items-center rounded-lg hover:bg-gray-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-orange-400">
            {menuOpen ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
        {menuOpen && <nav id="admin-mobile-navigation" aria-label="Admin navigation" className="max-h-[calc(100dvh-3.5rem)] overflow-y-auto border-t border-gray-700 p-2">
          {adminLinks.map(([label, path]) => <Link key={path} to={path} onClick={() => setMenuOpen(false)} className="flex min-h-11 items-center rounded-lg px-3 text-gray-200 hover:bg-gray-800 hover:text-white">{label}</Link>)}
        </nav>}
      </header>}
      <Outlet />
    </div>
  );
}

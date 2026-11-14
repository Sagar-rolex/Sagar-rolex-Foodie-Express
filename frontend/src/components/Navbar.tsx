import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { ShoppingCart, LogOut, Menu, X, Search, Heart } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { authApi } from '../api/authApi';
import { cartApi } from '../api/cartApi';
import { toast } from 'sonner';

export default function Navbar() {
  const { isAuthenticated, user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMobileMenuOpen(false);
    setProfileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!profileMenuOpen) return;
    const closeOnOutsideClick = (event: PointerEvent) => {
      if (!profileMenuRef.current?.contains(event.target as Node)) setProfileMenuOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setProfileMenuOpen(false);
    };
    document.addEventListener('pointerdown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [profileMenuOpen]);

  useEffect(() => {
    if (isAdmin) {
      setCartCount(0);
      return;
    }
    let latestRequest = 0;
    const updateCartCount = async () => {
      const requestId = ++latestRequest;
      try {
        const response = await cartApi.getCart();
        if (requestId === latestRequest) setCartCount(response.data.cart.items.reduce((sum, item) => sum + item.quantity, 0));
      } catch {
        if (requestId === latestRequest) setCartCount(0);
      }
    };
    updateCartCount();
    window.addEventListener('cart:updated', updateCartCount);
    return () => window.removeEventListener('cart:updated', updateCartCount);
  }, [isAuthenticated, isAdmin, user?.userId]);

  const handleLogout = async () => {
    try {
      await authApi.logout();
      logout();
      toast.success('Logged out successfully');
      navigate('/');
    } catch (error) {
      toast.error('Failed to logout');
    }
  };

  return (
    <nav className="relative bg-white border-b border-gray-100 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-20">
          {/* Logo with Dashed Underline */}
          <Link to="/" className="flex items-center space-x-2 group">
            <span className="text-2xl">🍲</span>
            <div className="flex flex-col">
              <span className="font-extrabold text-xl tracking-tight text-gray-900 group-hover:text-[#F05A28] transition">
                Foodie Express<span className="text-[#F05A28]">.</span>
              </span>
              <div className="w-full border-b-2 border-dashed border-[#F05A28]"></div>
            </div>
          </Link>

          {/* Desktop Navigation Links */}
          <div className="hidden xl:flex items-center space-x-5 2xl:space-x-8">
            <Link to="/" className="text-gray-700 hover:text-[#F05A28] font-medium transition">
              Home
            </Link>
            <Link to="/restaurants" className="text-gray-700 hover:text-[#F05A28] font-medium transition">
              Menu
            </Link>
            <Link to="/" className="text-gray-700 hover:text-[#F05A28] font-medium transition">
              About Us
            </Link>
            <Link to="/faqs" className="text-gray-700 hover:text-[#F05A28] font-medium transition">Help</Link>
          </div>

          {/* Right Actions: Search & User / Cart */}
          <div className="hidden xl:flex items-center space-x-4 2xl:space-x-6">
            <Link to="/restaurants" className="text-gray-600 hover:text-[#F05A28] transition">
              <Search size={22} />
            </Link>

            {!isAdmin && (
              <Link to="/cart" className="relative text-gray-700 hover:text-[#F05A28] transition flex items-center space-x-1">
                <ShoppingCart size={22} />
                <span className="text-sm font-semibold">Cart</span>
                {cartCount > 0 && <span className="absolute -right-3 -top-2 flex h-5 min-w-5 items-center justify-center rounded-full bg-[#F05A28] px-1 text-xs font-bold text-white">{cartCount}</span>}
              </Link>
            )}

            {isAuthenticated && !isAdmin && (
              <>
                <Link to="/orders" className="text-gray-700 hover:text-[#F05A28] font-medium transition">
                  Orders
                </Link>
                <Link to="/wishlist" className="flex items-center gap-1 text-gray-700 hover:text-[#F05A28] font-medium transition"><Heart size={17}/>Wishlist</Link>
              </>
            )}

            {!isAuthenticated && (
              <div className="flex items-center space-x-3">
                <Link to="/login" className="text-gray-700 hover:text-[#F05A28] font-semibold transition px-4 py-2">
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="bg-[#F05A28] text-white px-5 py-2.5 rounded-xl font-semibold hover:bg-orange-600 transition shadow-md shadow-orange-500/20"
                >
                  Sign Up
                </Link>
              </div>
            )}

            {isAdmin && (
              <div className="flex items-center space-x-4">
                <Link to="/admin/dashboard" className="text-[#F05A28] font-bold text-sm bg-orange-50 px-3 py-1.5 rounded-lg border border-orange-200">
                  Admin Panel
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-gray-500 hover:text-red-600 transition"
                >
                  <LogOut size={20} />
                </button>
              </div>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            type="button"
            className="grid h-11 w-11 shrink-0 place-items-center rounded-lg text-gray-700 hover:bg-orange-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-orange-600 xl:hidden"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label={mobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={mobileMenuOpen}
            aria-controls="mobile-navigation"
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {isAuthenticated && !isAdmin && (
          <div ref={profileMenuRef} className="absolute right-4 top-1/2 hidden -translate-y-1/2 xl:block">
            <button
              type="button"
              onClick={() => setProfileMenuOpen(open => !open)}
              aria-label={`Open profile menu${user?.username ? ` for ${user.username}` : ''}`}
              aria-haspopup="menu"
              aria-expanded={profileMenuOpen}
              title={user?.username || 'Your profile'}
              className="flex h-12 w-12 items-center justify-center rounded-full bg-[#F05A28] text-sm font-bold text-white transition hover:bg-orange-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-orange-600"
            >
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </button>
            {profileMenuOpen && (
              <div role="menu" aria-label="Profile menu" className="absolute right-0 top-full z-50 mt-3 w-56 overflow-hidden rounded-xl border border-gray-100 bg-white py-2 shadow-xl">
                <div className="border-b border-gray-100 px-4 py-3">
                  <p className="truncate text-sm font-semibold text-gray-900">{user?.username || 'Your account'}</p>
                  <p className="text-xs text-gray-500">Customer account</p>
                </div>
                <Link to="/profile" role="menuitem" onClick={() => setProfileMenuOpen(false)} className="block px-4 py-3 text-sm text-gray-700 hover:bg-orange-50 hover:text-[#F05A28]">
                  My Profile
                </Link>
                <button
                  type="button"
                  role="menuitem"
                  onClick={() => { setProfileMenuOpen(false); void handleLogout(); }}
                  className="flex w-full items-center gap-2 px-4 py-3 text-left text-sm font-medium text-red-600 hover:bg-red-50"
                >
                  <LogOut size={17} />
                  Logout
                </button>
              </div>
            )}
          </div>
        )}

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div id="mobile-navigation" className="max-h-[calc(100dvh-5rem)] overflow-y-auto border-t border-gray-100 py-3 xl:hidden">
            <nav aria-label="Mobile navigation" className="space-y-1">
            <Link
              to="/"
              className="flex min-h-11 items-center rounded-lg px-3 text-gray-700 hover:bg-orange-50 hover:text-[#F05A28] font-medium"
              onClick={() => setMobileMenuOpen(false)}
            >
              Home
            </Link>
            <Link
              to="/restaurants"
              className="flex min-h-11 items-center rounded-lg px-3 text-gray-700 hover:bg-orange-50 hover:text-[#F05A28] font-medium"
              onClick={() => setMobileMenuOpen(false)}
            >
              Menu
            </Link>
            <Link to="/restaurants" className="flex min-h-11 items-center gap-2 rounded-lg px-3 font-medium text-gray-700 hover:bg-orange-50 hover:text-[#F05A28]" onClick={() => setMobileMenuOpen(false)}><Search size={18}/> Search restaurants</Link>
            <Link to="/" className="flex min-h-11 items-center rounded-lg px-3 font-medium text-gray-700 hover:bg-orange-50 hover:text-[#F05A28]" onClick={() => setMobileMenuOpen(false)}>About Us</Link>
            <Link to="/faqs" className="block py-1 font-medium text-gray-700 hover:text-[#F05A28]" onClick={() => setMobileMenuOpen(false)}>Help & FAQs</Link>

            {!isAdmin && (
              <Link to="/cart" className="flex min-h-11 items-center rounded-lg px-3 text-gray-700 hover:bg-orange-50 hover:text-[#F05A28] font-medium" onClick={() => setMobileMenuOpen(false)}>
                Cart {cartCount > 0 && `(${cartCount})`}
              </Link>
            )}

            {isAuthenticated && !isAdmin && (
              <>
                <Link
                  to="/orders"
                  className="flex min-h-11 items-center rounded-lg px-3 text-gray-700 hover:bg-orange-50 hover:text-[#F05A28] font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Orders
                </Link>
                <Link to="/wishlist" className="flex min-h-11 items-center rounded-lg px-3 font-medium text-gray-700 hover:bg-orange-50 hover:text-[#F05A28]" onClick={() => setMobileMenuOpen(false)}>Wishlist</Link>
                <Link
                  to="/profile"
                  className="flex min-h-11 items-center rounded-lg px-3 text-gray-700 hover:bg-orange-50 hover:text-[#F05A28] font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Profile ({user?.username})
                </Link>
                <button
                  onClick={() => {
                    handleLogout();
                    setMobileMenuOpen(false);
                  }}
                  className="flex min-h-11 items-center rounded-lg px-3 text-red-600 font-medium hover:bg-red-50"
                >
                  Logout
                </button>
              </>
            )}

            {!isAuthenticated && (
              <div className="pt-2 flex flex-col space-y-2">
                <Link
                  to="/login"
                  className="flex min-h-11 items-center justify-center rounded-xl border border-gray-200 py-2 text-center text-gray-700 font-semibold"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="flex min-h-11 items-center justify-center rounded-xl bg-[#F05A28] py-2 text-center text-white font-semibold shadow-md"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Sign Up
                </Link>
              </div>
            )}

            {isAdmin && (
              <Link
                to="/admin/dashboard"
                className="flex min-h-11 items-center rounded-lg px-3 text-[#F05A28] font-semibold hover:bg-orange-50"
                onClick={() => setMobileMenuOpen(false)}
              >
                Admin Panel
              </Link>
            )}
            </nav>
          </div>
        )}
      </div>
    </nav>
  );
}

import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { AuthProvider } from './contexts/AuthContext'
import { Toaster } from 'sonner'

// Layouts
import MainLayout from './layouts/MainLayout'
import AdminLayout from './layouts/AdminLayout'

// Public pages
const Home = lazy(() => import('./pages/Home'))
const Login = lazy(() => import('./pages/Login'))
const Register = lazy(() => import('./pages/Register'))
const Restaurants = lazy(() => import('./pages/Restaurants'))
const RestaurantDetails = lazy(() => import('./pages/RestaurantDetails'))
const OrderConfirmation = lazy(() => import('./pages/OrderConfirmation'))

// Protected pages (customer)
const Profile = lazy(() => import('./pages/Profile'))
const Cart = lazy(() => import('./pages/Cart'))
const Checkout = lazy(() => import('./pages/Checkout'))
const Orders = lazy(() => import('./pages/Orders'))
const OrderDetails = lazy(() => import('./pages/OrderDetails'))
const Bill = lazy(() => import('./pages/Bill'))

// Admin pages
const AdminLogin = lazy(() => import('./pages/admin/AdminLogin'))
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'))
const AdminRestaurants = lazy(() => import('./pages/admin/AdminRestaurants'))
const AdminFoodItems = lazy(() => import('./pages/admin/AdminFoodItems'))
const AdminOrders = lazy(() => import('./pages/admin/AdminOrders'))
const AdminCustomers = lazy(() => import('./pages/admin/AdminCustomers'))
const AdminCoupons = lazy(() => import('./pages/admin/AdminCoupons'))
const AdminSupport = lazy(() => import('./pages/admin/AdminSupport'))
const FAQs = lazy(() => import('./pages/FAQs'))
const SupportTickets = lazy(() => import('./pages/SupportTickets'))
const Wishlist = lazy(() => import('./pages/Wishlist'))

// Protected routes
import ProtectedRoute from './components/ProtectedRoute'
import AdminRoute from './components/AdminRoute'

function App() {
  return (
    <AuthProvider>
      <Router>
        <Toaster richColors position="top-right" />
        <Suspense fallback={<div className="grid min-h-[50vh] place-items-center px-4 text-sm text-gray-500" role="status" aria-live="polite">Loading page…</div>}>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<MainLayout />}>
            <Route index element={<Home />} />
            <Route path="login" element={<Login />} />
            <Route path="register" element={<Register />} />
            <Route path="restaurants" element={<Restaurants />} />
            <Route path="restaurants/:id" element={<RestaurantDetails />} />
            <Route path="faqs" element={<FAQs />} />

            {/* Protected customer routes */}
            <Route path="profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
            <Route path="cart" element={<Cart />} />
            <Route path="checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
            <Route path="orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
            <Route path="order-confirmation/:orderId" element={<ProtectedRoute><OrderConfirmation /></ProtectedRoute>} />
            <Route path="orders/:id" element={<ProtectedRoute><OrderDetails /></ProtectedRoute>} />
            <Route path="bills/:id" element={<ProtectedRoute><Bill /></ProtectedRoute>} />
            <Route path="support" element={<ProtectedRoute><SupportTickets /></ProtectedRoute>} />
            <Route path="wishlist" element={<ProtectedRoute><Wishlist /></ProtectedRoute>} />
          </Route>

          {/* Admin routes */}
          <Route path="/admin" element={<AdminLayout />}>
            <Route path="login" element={<AdminLogin />} />
            <Route path="dashboard" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
            <Route path="restaurants" element={<AdminRoute><AdminRestaurants /></AdminRoute>} />
            <Route path="food-items" element={<AdminRoute><AdminFoodItems /></AdminRoute>} />
            <Route path="orders" element={<AdminRoute><AdminOrders /></AdminRoute>} />
            <Route path="customers" element={<AdminRoute><AdminCustomers /></AdminRoute>} />
            <Route path="coupons" element={<AdminRoute><AdminCoupons /></AdminRoute>} />
            <Route path="support" element={<AdminRoute><AdminSupport /></AdminRoute>} />
            <Route index element={<Navigate to="/admin/dashboard" replace />} />
          </Route>

          {/* Catch-all route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </Suspense>
      </Router>
    </AuthProvider>
  )
}

export default App

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Navigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { authApi } from '../api/authApi';
import { toast } from 'sonner';
import { LoginRequest } from '../types';
import { Mail, Lock, Eye, EyeOff, ArrowRight, ShieldCheck, Clock } from 'lucide-react';

export default function Login() {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>();
  const { login, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);


  if (isAuthenticated) {
    return <Navigate to={isAdmin ? '/admin/dashboard' : '/restaurants'} replace />;
  }

  const onSubmit = async (data: LoginRequest) => {
    setIsLoading(true);
    try {
      const response = await authApi.login({
        email: data.email,
        password: data.password,
      });

      if (response.data) {
        await login(response.data);
        toast.success(`Welcome back, ${response.data.username || 'Foodie'}!`);

        // If the authenticated user is an ADMIN, route to dashboard
        if (response.data.role === 'ADMIN') {
          navigate('/admin/dashboard', { replace: true });
        } else {
          // Route back to previous attempted page or /restaurants
          const destination = location.state?.from?.pathname || '/restaurants';
          navigate(destination, { replace: true });
        }
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'The email or password you entered is incorrect.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-gray-50">
      {/* Left Column: Visual Food Branding */}
      <div className="relative hidden lg:flex lg:w-1/2 bg-gradient-to-br from-orange-600 via-amber-600 to-red-600 p-12 text-white flex-col justify-between overflow-hidden">
        <div className="absolute inset-0 bg-[url('/images/shared-meal.jpg')] bg-cover bg-center mix-blend-overlay opacity-30"></div>
        <div className="relative z-10">
          <Link to="/" className="inline-flex items-center space-x-2 text-2xl font-bold tracking-tight">
            <span className="text-3xl">🍲</span>
            <span>Foodie Express</span>
          </Link>
          <div className="mt-20 max-w-md">
            <h1 className="text-4xl font-extrabold leading-tight">
              Delicious food from your favorite spots, delivered fast.
            </h1>
            <p className="mt-4 text-orange-100 text-lg">
              Explore 100+ dishes across North Indian, South Indian, Chinese, street food delicacies, and international treats.
            </p>
          </div>
        </div>

        <div className="relative z-10 grid grid-cols-2 gap-4 max-w-md mt-8">
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 border border-white/20">
            <ShieldCheck className="h-6 w-6 text-white mb-2" />
            <h3 className="font-semibold text-sm">Safe & Secure</h3>
            <p className="text-xs text-orange-100 mt-1">Encrypted authentication and protected customer data</p>
          </div>
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 border border-white/20">
            <Clock className="h-6 w-6 text-white mb-2" />
            <h3 className="font-semibold text-sm">30-Min Delivery</h3>
            <p className="text-xs text-orange-100 mt-1">Hot, fresh meals right to your doorstep</p>
          </div>
        </div>

        <div className="relative z-10 text-xs text-orange-200 mt-8">
          © {new Date().getFullYear()} Foodie Express. All rights reserved.
        </div>
      </div>

      {/* Right Column: Customer Login Form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md space-y-8 bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
          <div>
            <div className="lg:hidden flex items-center space-x-2 mb-6">
              <span className="text-2xl">🍲</span>
              <span className="text-xl font-bold text-gray-900">Foodie Express</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">Welcome back</h2>
            <p className="text-sm text-gray-500 mt-2">
              Sign in with your email address to access your cart, favorites, and orders.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email address</label>
              <div className="relative">
                <Mail className="absolute left-3.5 top-3.5 text-gray-400" size={18} />
                <input
                  {...register('email', {
                    required: 'Email address is required',
                    pattern: {
                      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                      message: 'Please enter a valid email address'
                    }
                  })}
                  type="email"
                  autoComplete="email"
                  className={`w-full pl-10 pr-4 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.email ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="name@example.com"
                />
              </div>
              {errors.email && <p className="text-red-600 text-xs mt-1.5 font-medium">{errors.email.message}</p>}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-sm font-medium text-gray-700">Password</label>
              </div>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3.5 text-gray-400" size={18} />
                <input
                  {...register('password', {
                    required: 'Password is required'
                  })}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className={`w-full pl-10 pr-11 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.password ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3.5 text-gray-400 hover:text-gray-600 focus:outline-none"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && <p className="text-red-600 text-xs mt-1.5 font-medium">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-orange-600 text-white py-3 rounded-xl hover:bg-orange-700 active:scale-[0.99] transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed font-semibold shadow-md flex items-center justify-center space-x-2"
            >
              {isLoading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

          <div className="pt-4 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-gray-600">
            <div>
              Don't have an account?{' '}
              <Link to="/register" className="text-orange-600 hover:text-orange-700 font-semibold">
                Sign up
              </Link>
            </div>
            <Link to="/admin/login" className="text-xs text-gray-400 hover:text-gray-700 transition">
              Administrator sign in →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Navigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { authApi } from '../../api/authApi';
import { toast } from 'sonner';
import { LoginRequest } from '../../types';
import { Shield, Mail, Lock, Eye, EyeOff, ArrowRight, ArrowLeft } from 'lucide-react';

export default function AdminLogin() {
  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>();
  const { login, isAuthenticated, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const redirectMessage = location.state?.message;

  if (isAuthenticated && isAdmin) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  const onSubmit = async (data: LoginRequest) => {
    setIsLoading(true);
    try {
      // Use dedicated admin authentication endpoint
      const response = await authApi.adminLogin({
        email: data.email,
        password: data.password,
      });

      if (response.data && response.data.role === 'ADMIN') {
        await login(response.data);
        toast.success(`Welcome back, Administrator ${response.data.username || ''}!`);
        navigate('/admin/dashboard', { replace: true });
      } else {
        toast.error('Access denied. Administrator privileges are required.');
      }
    } catch (error: any) {
      const status = error.response?.status;
      if (status === 403) {
        toast.error('Access denied. This account does not have administrator privileges.');
      } else if (status === 429) {
        toast.error('Too many failed login attempts. Please wait a few minutes before trying again.');
      } else {
        const message = error.response?.data?.message || 'Invalid administrator email or password.';
        toast.error(message);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-gray-900 to-zinc-900 flex items-center justify-center py-12 px-4 sm:px-6">
      <div className="w-full max-w-md">
        {/* Top Branding */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-orange-500/10 border border-orange-500/30 rounded-2xl mb-4 text-orange-400">
            <Shield size={32} />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">Admin Console</h1>
          <p className="text-gray-400 text-sm mt-1.5">Foodie Express Management System</p>
        </div>

        {/* Card */}
        <div className="bg-gray-900/80 backdrop-blur-xl border border-gray-800 rounded-2xl shadow-2xl p-8 space-y-6">
          {redirectMessage && (
            <div className="p-3.5 bg-amber-950/50 border border-amber-800/60 rounded-xl text-xs text-amber-300 flex items-start space-x-2">
              <span className="font-bold">⚠️</span>
              <div>{redirectMessage}</div>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                Administrator Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3.5 top-3.5 text-gray-500" size={18} />
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
                  className={`w-full pl-10 pr-4 py-2.5 bg-gray-950/70 border rounded-xl text-white placeholder-gray-500 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition text-sm ${
                    errors.email ? 'border-red-500/50 bg-red-950/20' : 'border-gray-800'
                  }`}
                  placeholder="admin@foodie.com"
                />
              </div>
              {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3.5 text-gray-500" size={18} />
                <input
                  {...register('password', {
                    required: 'Password is required'
                  })}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className={`w-full pl-10 pr-11 py-2.5 bg-gray-950/70 border rounded-xl text-white placeholder-gray-500 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition text-sm ${
                    errors.password ? 'border-red-500/50 bg-red-950/20' : 'border-gray-800'
                  }`}
                  placeholder="••••••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3.5 text-gray-500 hover:text-gray-300 focus:outline-none"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.password && <p className="text-red-400 text-xs mt-1.5">{errors.password.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-gradient-to-r from-orange-600 to-amber-600 text-white py-3 rounded-xl hover:from-orange-500 hover:to-amber-500 active:scale-[0.99] transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed font-semibold shadow-lg shadow-orange-950/40 flex items-center justify-center space-x-2 text-sm mt-4"
            >
              {isLoading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Authenticating...</span>
                </>
              ) : (
                <>
                  <span>Sign In as Administrator</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>

          <div className="pt-4 border-t border-gray-800/80 text-center">
            <Link
              to="/"
              className="inline-flex items-center space-x-2 text-xs text-gray-400 hover:text-gray-200 transition"
            >
              <ArrowLeft size={14} />
              <span>Return to Customer Storefront</span>
            </Link>
          </div>
        </div>

        <div className="mt-8 text-center text-xs text-gray-600">
          Restricted system. Unauthorized access attempts are monitored and logged.
        </div>
      </div>
    </div>
  );
}

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Navigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { authApi } from '../api/authApi';
import { toast } from 'sonner';
import { RegisterRequest } from '../types';
import { User, Mail, Lock, Phone, MapPin, Eye, EyeOff, ArrowRight, ShieldCheck, HeartHandshake } from 'lucide-react';

interface RegisterFormData extends RegisterRequest {
  confirmPassword?: string;
}

export default function Register() {
  const { register, handleSubmit, watch, formState: { errors } } = useForm<RegisterFormData>();
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/restaurants" replace />;
  }

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    try {
      const response = await authApi.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        phone: data.phone,
        address: data.address,
      });

      if (response.success && response.data) {
        // Auto-login with returned session token
        await login(response.data);
        toast.success(`Welcome to Foodie Express, ${data.fullName}!`);
        navigate('/restaurants', { replace: true });
      } else {
        toast.success('Registration successful! Please log in.');
        navigate('/login', { replace: true });
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Registration failed. Please verify your details.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-gray-50">
      {/* Left Column: Visual Food Branding */}
      <div className="relative hidden lg:flex lg:w-1/2 bg-gradient-to-br from-amber-600 via-orange-600 to-red-600 p-12 text-white flex-col justify-between overflow-hidden">
        <div className="absolute inset-0 bg-[url('/images/restaurant-interior.jpg')] bg-cover bg-center mix-blend-overlay opacity-30"></div>
        <div className="relative z-10">
          <Link to="/" className="inline-flex items-center space-x-2 text-2xl font-bold tracking-tight">
            <span className="text-3xl">🍲</span>
            <span>Foodie Express</span>
          </Link>
          <div className="mt-20 max-w-md">
            <h1 className="text-4xl font-extrabold leading-tight">
              Join thousands of happy foodies today.
            </h1>
            <p className="mt-4 text-orange-100 text-lg">
              Create your account to unlock personalized recommendations, track orders in real time, and enjoy express food delivery.
            </p>
          </div>
        </div>

        <div className="relative z-10 grid grid-cols-2 gap-4 max-w-md mt-8">
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 border border-white/20">
            <ShieldCheck className="h-6 w-6 text-white mb-2" />
            <h3 className="font-semibold text-sm">Safe & Private</h3>
            <p className="text-xs text-orange-100 mt-1">Your details and order history are strictly confidential</p>
          </div>
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 border border-white/20">
            <HeartHandshake className="h-6 w-6 text-white mb-2" />
            <h3 className="font-semibold text-sm">Best Deals</h3>
            <p className="text-xs text-orange-100 mt-1">Enjoy exclusive member discounts on top restaurants</p>
          </div>
        </div>

        <div className="relative z-10 text-xs text-orange-200 mt-8">
          © {new Date().getFullYear()} Foodie Express. All rights reserved.
        </div>
      </div>

      {/* Right Column: Customer Registration Form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-md space-y-6 bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
          <div>
            <div className="lg:hidden flex items-center space-x-2 mb-6">
              <span className="text-2xl">🍲</span>
              <span className="text-xl font-bold text-gray-900">Foodie Express</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-bold text-gray-900 tracking-tight">Create your account</h2>
            <p className="text-sm text-gray-500 mt-1">
              Start ordering mouthwatering dishes in just a couple minutes.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
              <div className="relative">
                <User className="absolute left-3.5 top-3.5 text-gray-400" size={18} />
                <input
                  {...register('fullName', {
                    required: 'Full name is required',
                    minLength: { value: 2, message: 'Name must be at least 2 characters' }
                  })}
                  type="text"
                  className={`w-full pl-10 pr-4 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.fullName ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="e.g., Alex Johnson"
                />
              </div>
              {errors.fullName && <p className="text-red-600 text-xs mt-1 font-medium">{errors.fullName.message}</p>}
            </div>

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
                  className={`w-full pl-10 pr-4 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.email ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="name@example.com"
                />
              </div>
              {errors.email && <p className="text-red-600 text-xs mt-1 font-medium">{errors.email.message}</p>}
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone (Optional)</label>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-3.5 text-gray-400" size={16} />
                  <input
                    {...register('phone')}
                    type="tel"
                    className="w-full pl-9 pr-3 py-2.5 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition text-sm"
                    placeholder="+91 9876543210"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">City / Area</label>
                <div className="relative">
                  <MapPin className="absolute left-3.5 top-3.5 text-gray-400" size={16} />
                  <input
                    {...register('address')}
                    type="text"
                    className="w-full pl-9 pr-3 py-2.5 border border-gray-200 rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition text-sm"
                    placeholder="e.g. Bandra, Mumbai"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3.5 text-gray-400" size={18} />
                <input
                  {...register('password', {
                    required: 'Password is required',
                    minLength: { value: 6, message: 'Password must be at least 6 characters' }
                  })}
                  type={showPassword ? 'text' : 'password'}
                  className={`w-full pl-10 pr-11 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.password ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="At least 6 characters"
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
              {errors.password && <p className="text-red-600 text-xs mt-1 font-medium">{errors.password.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
              <div className="relative">
                <Lock className="absolute left-3.5 top-3.5 text-gray-400" size={18} />
                <input
                  {...register('confirmPassword', {
                    required: 'Please confirm your password',
                    validate: (value) => value === password || 'Passwords do not match'
                  })}
                  type={showConfirmPassword ? 'text' : 'password'}
                  className={`w-full pl-10 pr-11 py-2.5 border rounded-xl text-gray-900 placeholder-gray-400 focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition ${
                    errors.confirmPassword ? 'border-red-300 bg-red-50/50' : 'border-gray-200'
                  }`}
                  placeholder="Re-enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3.5 top-3.5 text-gray-400 hover:text-gray-600 focus:outline-none"
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p className="text-red-600 text-xs mt-1 font-medium">{errors.confirmPassword.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-orange-600 text-white py-3 rounded-xl hover:bg-orange-700 active:scale-[0.99] transition duration-150 disabled:opacity-60 disabled:cursor-not-allowed font-semibold shadow-md flex items-center justify-center space-x-2 mt-4"
            >
              {isLoading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Creating Account...</span>
                </>
              ) : (
                <>
                  <span>Create Account</span>
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>

          <div className="pt-4 border-t border-gray-100 text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="text-orange-600 hover:text-orange-700 font-semibold">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

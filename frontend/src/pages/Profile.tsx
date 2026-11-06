import { useState, useEffect, useCallback } from 'react';
import { customerApi } from '../api/customerApi';
import { Customer } from '../types';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { User, Mail, Phone, MapPin, LogOut, CheckCircle } from 'lucide-react';

interface ProfileForm {
  fullName: string;
  phone: string;
  address: string;
}

export default function Profile() {
  const [customer, setCustomer] = useState<Customer | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const navigate = useNavigate();
  const { register, handleSubmit, reset, formState: { errors } } = useForm<ProfileForm>();

  const fetchProfile = useCallback(async () => {
    try {
      const response = await customerApi.getProfile();
      setCustomer(response.data);
      reset({
        fullName: response.data.fullName,
        phone: response.data.phone || '',
        address: response.data.address || '',
      });
    } catch (error: any) {
      if (error.response?.status === 401) {
        toast.error('Please login to view profile');
        navigate('/login');
      } else {
        toast.error('Failed to load profile');
      }
    } finally {
      setLoading(false);
    }
  }, [navigate, reset]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const onSubmit = async (data: ProfileForm) => {
    setSaving(true);
    try {
      const response = await customerApi.updateProfile(data);
      setCustomer(response.data);
      setIsEditing(false);
      toast.success('Profile updated successfully!');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
        <div className="animate-pulse">
          <div className="h-12 w-12 bg-gray-200 rounded-full mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Loading your profile...</h2>
        </div>
      </div>
    );
  }

  if (!customer) {
    return (
      <div className="min-h-[80vh] flex flex-col items-center justify-center px-4">
        <div className="text-center">
          <LogOut size={48} className="mx-auto text-gray-400 mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Please log in to continue</h2>
          <p className="text-gray-600 mb-6">You need to be logged in to view your profile.</p>
          <div className="flex space-x-3">
            <button
              onClick={() => navigate('/login')}
              className="bg-gray-200 text-gray-800 px-4 py-2 rounded-lg hover:bg-gray-300 transition"
            >
              Login
            </button>
            <button
              onClick={() => navigate('/register')}
              className="bg-primary text-white px-4 py-2 rounded-lg font-semibold hover:bg-primary/90 transition"
            >
              Create Account
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[80vh]">
      {/* Desktop Layout */}
      <div className="hidden md:block max-w-4xl mx-auto px-4 py-8">
        <div className="bg-white rounded-xl shadow-lg p-8">
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold text-gray-900">My Profile</h1>
            <div className="flex space-x-3">
              <button
                onClick={() => setIsEditing(!isEditing)}
                className={isEditing ? 'text-gray-600 hover:text-gray-700' : 'text-blue-600 hover:text-blue-700 font-medium'}
              >
                {isEditing ? 'Cancel' : 'Edit Profile'}
              </button>
            </div>
          </div>

          {/* Profile Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Profile Info */}
            <div className="space-y-6">
              {/* Avatar */}
              <div className="flex items-center mb-6">
                <div className="w-16 h-16 bg-gradient-to-r from-primary to-primary/90 rounded-full flex items-center justify-center text-white text-lg font-bold">
                  {customer.fullName.charAt(0).toUpperCase()}
                </div>
                <div className="ml-4">
                  <h2 className="text-xl font-bold text-gray-900">{customer.fullName}</h2>
                  <p className="text-sm text-gray-500">Customer since {new Date().getFullYear()}</p>
                </div>
              </div>

              {/* Details */}
              <div className="space-y-4">
                <div className="flex items-start space-x-3">
                  <User className="text-gray-400 mt-1" size={20} />
                  <div>
                    <p className="text-sm text-gray-500">Full Name</p>
                    <p className="font-medium text-gray-900">{customer.fullName}</p>
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <Mail className="text-gray-400 mt-1" size={20} />
                  <div>
                    <p className="text-sm text-gray-500">Email</p>
                    <p className="font-medium text-gray-900">{customer.user.email}</p>
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <Phone className="text-gray-400 mt-1" size={20} />
                  <div>
                    <p className="text-sm text-gray-500">Phone</p>
                    <p className="font-medium text-gray-900">{customer.phone || 'Not provided'}</p>
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <MapPin className="text-gray-400 mt-1" size={20} />
                  <div>
                    <p className="text-sm text-gray-500">Address</p>
                    <p className="font-medium text-gray-900">{customer.address || 'Not provided'}</p>
                  </div>
                </div>
              </div>

              {/* Actions */}
              {isEditing ? (
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Full Name</label>
                    <input
                      {...register('fullName', { required: 'Full name is required' })}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                    />
                    {errors.fullName && <p className="text-red-500 text-sm mt-1">{errors.fullName.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Phone Number</label>
                    <input
                      {...register('phone')}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Address</label>
                    <textarea
                      {...register('address')}
                      rows={3}
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                    />
                  </div>

                  <div className="flex items-center pt-4">
                    <button
                      type="submit"
                      disabled={saving}
                      className="w-full bg-primary text-white py-3 px-4 rounded-lg font-semibold hover:bg-primary/90 transition"
                    >
                      {saving ? (
                        <>
                          <span className="mr-2">Saving...</span>
                          <span className="animate-pulse inline-block h-4 w-4 border-2 border-primary rounded-full"></span>
                        </>
                      ) : (
                        <>
                          <span className="mr-2">Save Changes</span>
                          <CheckCircle size={16} className="ml-2" />
                        </>
                      )}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="flex space-x-4 mt-6">
                  <button
                    onClick={() => navigate('/')}
                    className="text-gray-600 hover:text-gray-700"
                  >
                    Continue to Home
                  </button>
                  <button
                    onClick={() => navigate('/orders')}
                    className="bg-primary text-white px-4 py-2 rounded-lg font-semibold hover:bg-primary/90 transition"
                  >
                    View Orders
                  </button>
                </div>
              )}
            </div>

            {/* Stats */}
            <div className="bg-gray-50 rounded-lg p-6">
              <h2 className="text-xl font-bold text-gray-900 mb-4">Your Stats</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="text-center">
                  <p className="text-sm text-gray-500">Total Orders</p>
                  <p className="text-2xl font-bold text-gray-900">0</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-gray-500">Total Saved</p>
                  <p className="text-2xl font-bold text-gray-900">₹0</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-gray-500">Favorite Cuisine</p>
                  <p className="text-2xl font-bold text-gray-900">Indian</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-gray-500">Avg. Rating</p>
                  <p className="text-2xl font-bold text-gray-900">4.8</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile Layout */}
      <div className="md:hidden bg-white px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">My Profile</h1>

        {/* Avatar */}
        <div className="flex items-center mb-6">
          <div className="w-16 h-16 bg-gradient-to-r from-primary to-primary/90 rounded-full flex items-center justify-center text-white text-lg font-bold">
            {customer.fullName.charAt(0).toUpperCase()}
          </div>
          <div className="ml-4">
            <h2 className="text-xl font-bold text-gray-900">{customer.fullName}</h2>
            <p className="text-sm text-gray-500">Customer since {new Date().getFullYear()}</p>
          </div>
        </div>

        {/* Details */}
        <div className="space-y-4">
          <div className="flex items-start space-x-3">
            <User className="text-gray-400 mt-1" size={20} />
            <div>
              <p className="text-sm text-gray-500">Full Name</p>
              <p className="font-medium text-gray-900">{customer.fullName}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Mail className="text-gray-400 mt-1" size={20} />
            <div>
              <p className="text-sm text-gray-500">Email</p>
              <p className="font-medium text-gray-900">{customer.user.email}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <Phone className="text-gray-400 mt-1" size={20} />
            <div>
              <p className="text-sm text-gray-500">Phone</p>
              <p className="font-medium text-gray-900">{customer.phone || 'Not provided'}</p>
            </div>
          </div>

          <div className="flex items-start space-x-3">
            <MapPin className="text-gray-400 mt-1" size={20} />
            <div>
              <p className="text-sm text-gray-500">Address</p>
              <p className="font-medium text-gray-900">{customer.address || 'Not provided'}</p>
            </div>
          </div>
        </div>

        {/* Edit Button */}
        {isEditing ? (
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Full Name</label>
              <input
                {...register('fullName', { required: 'Full name is required' })}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              />
              {errors.fullName && <p className="text-red-500 text-sm mt-1">{errors.fullName.message}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Phone Number</label>
              <input
                {...register('phone')}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Address</label>
              <textarea
                {...register('address')}
                rows={3}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
              />
            </div>

            <div className="flex items-center pt-4">
              <button
                type="submit"
                disabled={saving}
                className="w-full bg-primary text-white py-3 px-4 rounded-lg font-semibold hover:bg-primary/90 transition"
              >
                {saving ? (
                  <>
                    <span className="mr-2">Saving...</span>
                    <span className="animate-pulse inline-block h-4 w-4 border-2 border-primary rounded-full"></span>
                  </>
                ) : (
                  <>
                    <span className="mr-2">Save Changes</span>
                    <CheckCircle size={16} className="ml-2" />
                  </>
                )}
              </button>
            </div>
          </form>
        ) : (
          <div className="flex justify-center mt-6">
            <button
              onClick={() => setIsEditing(!isEditing)}
              className="w-full bg-primary text-white py-3 px-4 rounded-lg font-semibold hover:bg-primary/90 transition"
            >
              {isEditing ? 'Cancel' : 'Edit Profile'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

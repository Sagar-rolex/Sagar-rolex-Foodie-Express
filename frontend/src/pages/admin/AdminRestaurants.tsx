import { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { Restaurant, RestaurantRequest } from '../../types';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Store } from 'lucide-react';
import SafeImage from '../../components/SafeImage';

export default function AdminRestaurants() {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingRestaurant, setEditingRestaurant] = useState<Restaurant | null>(null);
  const { register, handleSubmit, reset, watch, setValue, formState: { errors } } = useForm<RestaurantRequest>();
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const bannerImagePath = watch('bannerImagePath');

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const fetchRestaurants = async () => {
    try {
      const response = await adminApi.getRestaurants();
      setRestaurants(response.data);
    } catch (error) {
      toast.error('Failed to load restaurants');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingRestaurant(null);
    reset({ name: '', address: '', cuisineType: '', description: '', imagePath: '', bannerImagePath: '' });
    setShowModal(true);
  };

  const handleEdit = (restaurant: Restaurant) => {
    setEditingRestaurant(restaurant);
    reset({
      name: restaurant.name,
      address: restaurant.address,
      cuisineType: restaurant.cuisineType,
      description: restaurant.description,
      imagePath: restaurant.imagePath || '',
      bannerImagePath: restaurant.bannerImagePath || '',
    });
    setShowModal(true);
  };

  const handleToggleActive = async (restaurant: Restaurant) => {
    if (restaurant.active && !window.confirm(`Deactivate ${restaurant.name}? It will be hidden from customer browsing while preserving its dishes and order history.`)) return;
    try {
      const response = await adminApi.setRestaurantActive(restaurant.id, !restaurant.active);
      setRestaurants(rows => rows.map(r => r.id === restaurant.id ? response.data : r));
      toast.success(`Restaurant ${response.data.active ? 'activated' : 'deactivated'}`);
    } catch (error: any) { toast.error(error.response?.data?.message || 'Failed to update restaurant status'); }
  };

  const handleBannerUpload = async (file?: File) => {
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      toast.error('Choose a JPEG, PNG, or WebP image');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Banner image must be 5 MB or smaller');
      return;
    }
    setUploadingBanner(true);
    try {
      const result = await adminApi.uploadRestaurantBanner(file);
      setValue('bannerImagePath', result.data.bannerImagePath, { shouldDirty: true, shouldValidate: true });
      toast.success('Banner uploaded');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to upload banner');
    } finally {
      setUploadingBanner(false);
    }
  };

  const onSubmit = async (data: RestaurantRequest) => {
    try {
      if (editingRestaurant) {
        const response = await adminApi.updateRestaurant(editingRestaurant.id, data);
        setRestaurants(restaurants.map(r => r.id === editingRestaurant.id ? response.data : r));
        toast.success('Restaurant updated successfully');
      } else {
        const response = await adminApi.createRestaurant(data);
        setRestaurants([...restaurants, response.data]);
        toast.success('Restaurant created successfully');
      }
      setShowModal(false);
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to save restaurant');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-4 sm:p-8 lg:ml-64">
        <div className="animate-pulse space-y-4">
          <div className="h-12 bg-gray-300 rounded w-1/4" />
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-48 bg-gray-300 rounded" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 lg:ml-64">
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
          <a href="/admin/dashboard" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">
            <span>📊</span>
            <span>Dashboard</span>
          </a>
          <a href="/admin/restaurants" className="flex items-center space-x-3 px-4 py-3 rounded-lg bg-gray-800 text-white">
            <Store size={20} />
            <span>Restaurants</span>
          </a>
          <a href="/admin/food-items" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">
            <span>🍴</span>
            <span>Food Items</span>
          </a>
          <a href="/admin/orders" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">
            <span>🛒</span>
            <span>Orders</span>
          </a>
          <a href="/admin/customers" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">Customers</a>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="min-w-0 p-4 sm:p-6 lg:p-8">
        <div className="mb-8 flex flex-col items-start gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">Restaurants</h1>
            <p className="text-gray-600">Manage your restaurant partners</p>
          </div>
          <button
            onClick={handleCreate}
            className="flex min-h-11 items-center space-x-2 rounded-lg bg-blue-600 px-4 py-3 text-white transition hover:bg-blue-700 sm:px-6"
          >
            <Plus size={20} />
            <span>Add Restaurant</span>
          </button>
        </div>

        {/* Restaurants Grid */}
        {restaurants.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <Store size={64} className="mx-auto text-gray-400 mb-4" />
            <h3 className="text-xl font-bold text-gray-900 mb-2">No restaurants yet</h3>
            <p className="text-gray-600 mb-4">Add your first restaurant to get started</p>
            <button
              onClick={handleCreate}
              className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition"
            >
              Add Restaurant
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {restaurants.map((restaurant) => (
              <div key={restaurant.id} className="bg-white rounded-lg shadow-md overflow-hidden">
                <SafeImage imagePath={restaurant.bannerImagePath || restaurant.imagePath} alt={`${restaurant.name} restaurant`} kind="restaurant" className="h-40 w-full" />
                <div className="p-4">
                  <h3 className="text-lg font-bold text-gray-900">{restaurant.name}</h3>
                  <p className="text-gray-600 text-sm">{restaurant.cuisineType}</p>
                  <p className="text-gray-500 text-sm mt-1">{restaurant.address}</p>
                  <span className={`mt-2 inline-block rounded-full px-2 py-1 text-xs ${restaurant.active ? 'bg-green-100 text-green-800' : 'bg-gray-200 text-gray-700'}`}>{restaurant.active ? 'Active' : 'Inactive'}</span>
                  <div className="flex items-center space-x-2 mt-4">
                    <button
                      onClick={() => handleEdit(restaurant)}
                      className="flex items-center space-x-1 px-4 py-2 bg-blue-100 text-blue-600 rounded-lg hover:bg-blue-200 transition"
                    >
                      <Edit size={16} />
                      <span>Edit</span>
                    </button>
                    <button onClick={() => void handleToggleActive(restaurant)} className="rounded-lg bg-amber-50 px-3 py-2 text-amber-800 hover:bg-amber-100">{restaurant.active ? 'Deactivate' : 'Reactivate'}</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Modal */}
        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-3 sm:p-4">
            <div className="max-h-[calc(100dvh-1.5rem)] w-full max-w-md overflow-y-auto overscroll-contain rounded-lg bg-white p-4 shadow-xl sm:max-h-[calc(100dvh-2rem)] sm:p-6">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                {editingRestaurant ? 'Edit Restaurant' : 'Add Restaurant'}
              </h2>

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Restaurant Name</label>
                  <input
                    {...register('name', { required: 'Name is required' })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Enter restaurant name"
                  />
                  {errors.name && <p className="text-red-500 text-sm mt-1">{errors.name.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                  <input
                    {...register('address', { required: 'Address is required' })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Restaurant address"
                  />
                  {errors.address && <p className="text-red-500 text-sm mt-1">{errors.address.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Cuisine Type</label>
                  <input
                    {...register('cuisineType', { required: 'Cuisine type is required' })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="e.g., Indian, Chinese, Italian"
                  />
                  {errors.cuisineType && <p className="text-red-500 text-sm mt-1">{errors.cuisineType.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    {...register('description')}
                    rows={3}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Brief description of the restaurant"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Restaurant banner</label>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={(event) => { void handleBannerUpload(event.target.files?.[0]); event.target.value = ''; }}
                    disabled={uploadingBanner}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                  />
                  <input type="hidden" {...register('bannerImagePath', { pattern: { value: /^(|\/images\/[A-Za-z0-9_/-]+\.(jpg|jpeg|png|webp)|\/uploads\/restaurants\/[a-fA-F0-9-]+\.(jpg|png|webp))$/i, message: 'Upload a valid banner image' } })} />
                  <p className="mt-1 text-xs text-gray-500">Upload a JPEG, PNG, or WebP banner (up to 5 MB). It will appear on customer restaurant cards and details.</p>
                  {bannerImagePath && <SafeImage imagePath={bannerImagePath} alt="Restaurant banner preview" kind="restaurant" className="mt-2 h-32 w-full rounded-lg" />}
                  {errors.bannerImagePath && <p className="text-red-500 text-sm mt-1">{errors.bannerImagePath.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Local image path</label>
                  <input
                    {...register('imagePath', { pattern: { value: /^(|\/images\/[A-Za-z0-9_/-]+\.(jpg|jpeg|png|webp))$/i, message: 'Use a local path such as /images/restaurant-interior.jpg' } })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="/images/restaurant-interior.jpg"
                  />
                  <p className="mt-1 text-xs text-gray-500">Add the image under frontend/public/images. Leave blank to use a cuisine photo.</p>
                  {errors.imagePath && <p className="text-red-500 text-sm mt-1">{errors.imagePath.message}</p>}
                </div>

                <div className="flex space-x-4 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowModal(false)}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={uploadingBanner}
                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                  >
                    {uploadingBanner ? 'Uploading…' : editingRestaurant ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

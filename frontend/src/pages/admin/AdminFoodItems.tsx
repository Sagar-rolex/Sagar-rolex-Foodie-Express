import { useState, useEffect, useCallback } from 'react';
import { adminApi } from '../../api/adminApi';
import { Restaurant, FoodItem, FoodItemRequest } from '../../types';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { Plus, Edit, Trash2, Utensils, Store } from 'lucide-react';
import SafeImage from '../../components/SafeImage';
import { useWatch } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { FOOD_IMAGE_CATALOG } from '../../data/foodImageCatalog';



export default function AdminFoodItems() {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [foodItems, setFoodItems] = useState<FoodItem[]>([]);
  const [selectedRestaurant, setSelectedRestaurant] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingItem, setEditingItem] = useState<FoodItem | null>(null);
  const [deleting, setDeleting] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [availability, setAvailability] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [uploading, setUploading] = useState(false);
  const { register, handleSubmit, reset, setValue, control, formState: { errors, isSubmitting } } = useForm<FoodItemRequest>();
  const currentImagePath = useWatch({ control, name: 'imagePath' });
  const getFoodImagePath = (item: FoodItem) => {
    const restaurantName = restaurants.find(restaurant => restaurant.id === selectedRestaurant)?.name?.toLowerCase();
    const itemDefaultPath = restaurantName
      ? FOOD_IMAGE_CATALOG[`${restaurantName}||${item.name.trim().toLowerCase()}`]
      : undefined;
    if (itemDefaultPath) return itemDefaultPath;
    return item.imagePath || undefined;
  };

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(search.trim()), 250);
    return () => window.clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const fetchRestaurants = async () => {
    try {
      const response = await adminApi.getRestaurants();
      setRestaurants(response.data);
      const firstActive = response.data.find(r => r.active);
      if (firstActive) {
        setSelectedRestaurant(firstActive.id);
      }
    } catch (error) {
      toast.error('Failed to load restaurants');
    } finally {
      setLoading(false);
    }
  };

  const fetchFoodItems = useCallback(async () => {
    try {
      const response = await adminApi.getFoodItems({
        search: debouncedSearch || undefined,
        restaurantId: selectedRestaurant,
        available: availability === '' ? null : availability === 'true',
        page,
        size: 15,
      });
      setFoodItems(response.data.content);
      setTotalPages(response.data.totalPages);
    } catch (error) {
      toast.error('Failed to load food items');
    }
  }, [availability, page, debouncedSearch, selectedRestaurant]);

  useEffect(() => {
    fetchFoodItems();
  }, [fetchFoodItems]);

  const handleCreate = () => {
    if (!restaurants.some(restaurant => restaurant.id === selectedRestaurant && restaurant.active)) {
      toast.error('Select an active restaurant before adding a dish');
      return;
    }
    setEditingItem(null);
    reset({ name: '', description: '', price: 0, category: '', available: true, imagePath: '' });
    setShowModal(true);
  };

  const handleEdit = (item: FoodItem) => {
    setEditingItem(item);
    reset({
      name: item.name,
      description: item.description,
      price: Number(item.price),
      category: item.category,
      available: item.available,
      imagePath: getFoodImagePath(item) || '',
    });
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Deactivate this dish? It will disappear from customer menus while remaining in order history.')) return;

    setDeleting(id);
    try {
      await adminApi.deleteFoodItem(id);
      setFoodItems(foodItems.filter(item => item.id !== id));
      toast.success('Food item deactivated and removed from the customer menu');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete food item');
    } finally {
      setDeleting(null);
    }
  };

  const handleToggleAvailability = async (item: FoodItem) => {
    try {
      const response = await adminApi.updateFoodItem(item.id, {
        name: item.name,
        description: item.description,
        price: Number(item.price),
        category: item.category,
        available: !item.available,
      });
      setFoodItems(foodItems.map(f => f.id === item.id ? response.data : f));
      toast.success(`Item marked as ${!item.available ? 'available' : 'unavailable'}`);
    } catch (error: any) {
      toast.error('Failed to update availability');
    }
  };

  const onSubmit = async (data: FoodItemRequest) => {
    if (!selectedRestaurant) {
      toast.error('Please select a restaurant first');
      return;
    }

    try {
      if (editingItem) {
        const response = await adminApi.updateFoodItem(editingItem.id, data);
        setFoodItems(foodItems.map(item => item.id === editingItem.id ? response.data : item));
        toast.success('Food item updated successfully');
      } else {
        const response = await adminApi.createFoodItem(selectedRestaurant, data);
        setFoodItems([...foodItems, response.data]);
        toast.success('Food item created successfully');
      }
      setShowModal(false);
      fetchFoodItems();
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to save food item');
    }
  };

  const handleImageUpload = async (file?: File) => {
    if (!file) return;
    setUploading(true);
    try {
      const response = await adminApi.uploadFoodImage(file);
      setValue('imagePath', response.data.imagePath, { shouldDirty: true, shouldValidate: true });
      toast.success('Image uploaded and previewed');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Image upload failed');
    } finally {
      setUploading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 p-4 sm:p-8 lg:ml-64">
        <div className="animate-pulse space-y-4">
          <div className="h-12 bg-gray-300 rounded w-1/4" />
          <div className="h-64 bg-gray-300 rounded" />
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
          <a href="/admin/restaurants" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">
            <Store size={20} />
            <span>Restaurants</span>
          </a>
          <a href="/admin/food-items" className="flex items-center space-x-3 px-4 py-3 rounded-lg bg-gray-800 text-white">
            <Utensils size={20} />
            <span>Food Items</span>
          </a>
          <a href="/admin/orders" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition">
            <span>🛒</span>
            <span>Orders</span>
          </a>
          <Link to="/admin/customers" className="flex items-center space-x-3 px-4 py-3 rounded-lg hover:bg-gray-800 text-gray-300 hover:text-white transition"><span>Customers</span></Link>
        </nav>
      </aside>

      {/* Main Content */}
      <main className="min-w-0 p-4 sm:p-6 lg:p-8">
        <div className="mb-8 flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">Food Items</h1>
            <p className="text-gray-600">Manage menu items across restaurants</p>
          </div>
          <button
            onClick={handleCreate}
            disabled={!selectedRestaurant}
            className="flex min-h-11 w-full items-center justify-center space-x-2 rounded-lg bg-blue-600 px-4 py-3 text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50 sm:w-auto sm:px-6"
          >
            <Plus size={20} />
            <span>Add Food Item</span>
          </button>
        </div>

        {/* Restaurant Selector */}
        <div className="bg-white rounded-lg shadow-md p-4 mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">Select Restaurant</label>
          <select
            value={selectedRestaurant || ''}
            onChange={(e) => setSelectedRestaurant(Number(e.target.value))}
            className="w-full md:w-1/3 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
          >
            {restaurants.map((r) => (
              <option key={r.id} value={r.id} disabled={!r.active}>{r.name}{!r.active ? ' (inactive)' : ''}</option>
            ))}
          </select>
        </div>

        <div className="mb-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_220px_auto]">
          <input aria-label="Search food items" value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} placeholder="Search dishes or categories" className="rounded-lg border border-gray-300 bg-white px-4 py-2" />
          <select aria-label="Filter food availability" value={availability} onChange={e => { setAvailability(e.target.value); setPage(0); }} className="rounded-lg border border-gray-300 bg-white px-4 py-2">
            <option value="">All availability</option><option value="true">Available</option><option value="false">Unavailable</option>
          </select>
          <button onClick={() => fetchFoodItems()} className="rounded-lg bg-white px-4 py-2 text-gray-700 shadow">Refresh list</button>
        </div>

        {/* Food Items Table */}
        <div className="hidden overflow-hidden rounded-lg bg-white shadow-md lg:block">
          <table className="w-full min-w-[720px]">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Item</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Category</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Price</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Availability</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {foodItems.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                    No food items found for this restaurant
                  </td>
                </tr>
              ) : (
                foodItems.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <SafeImage imagePath={getFoodImagePath(item)} alt={`${item.name} banner`} className="aspect-video w-28 shrink-0 rounded-lg" />
                        <div>
                        <p className="font-medium text-gray-900">{item.name}</p>
                        <p className="text-sm text-gray-500">{item.description}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-gray-600">{item.category}</td>
                    <td className="px-6 py-4 font-semibold text-gray-900">₹{item.price}</td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => handleToggleAvailability(item)}
                        className={`px-3 py-1 rounded-full text-sm font-medium ${
                          item.available
                            ? 'bg-green-100 text-green-800 hover:bg-green-200'
                            : 'bg-red-100 text-red-800 hover:bg-red-200'
                        }`}
                      >
                        {item.available ? 'Available' : 'Unavailable'}
                      </button>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleEdit(item)}
                          className="p-2 text-blue-600 hover:bg-blue-100 rounded transition"
                        >
                          <Edit size={18} />
                        </button>
                        <button
                          onClick={() => handleDelete(item.id)}
                          disabled={deleting === item.id}
                          className="p-2 text-red-600 hover:bg-red-100 rounded transition disabled:opacity-50"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <div className="space-y-3 lg:hidden">
          {foodItems.length === 0 ? <div className="rounded-lg bg-white p-8 text-center text-gray-500">No food items found for this restaurant</div> : foodItems.map(item => <article key={item.id} className="rounded-xl bg-white p-4 shadow-sm">
            <div className="flex min-w-0 items-start gap-3"><SafeImage imagePath={getFoodImagePath(item)} alt={`${item.name} banner`} className="aspect-video w-28 shrink-0 rounded-lg"/><div className="min-w-0 flex-1"><h2 className="break-words font-semibold text-gray-900">{item.name}</h2><p className="mt-1 break-words text-sm text-gray-500">{item.description || item.category}</p><p className="mt-2 font-semibold text-gray-900">₹{item.price}</p><button onClick={() => handleToggleAvailability(item)} className={`mt-2 min-h-10 rounded-full px-3 py-1 text-sm font-medium ${item.available ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>{item.available ? 'Available' : 'Unavailable'}</button></div></div>
            <div className="mt-3 flex gap-2 border-t pt-3"><button onClick={() => handleEdit(item)} className="flex min-h-11 flex-1 items-center justify-center gap-2 rounded-lg bg-blue-50 px-3 py-2 text-blue-700"><Edit size={16}/>Edit</button><button onClick={() => void handleDelete(item.id)} disabled={deleting === item.id} className="flex min-h-11 flex-1 items-center justify-center gap-2 rounded-lg bg-red-50 px-3 py-2 text-red-700 disabled:opacity-50"><Trash2 size={16}/>{deleting === item.id ? 'Deleting…' : 'Delete'}</button></div>
          </article>)}
        </div>
        <div className="mt-4 flex items-center justify-between">
          <span className="text-sm text-gray-500">Page {totalPages ? page + 1 : 0} of {totalPages}</span>
          <div className="flex gap-2">
            <button disabled={page <= 0} onClick={() => setPage(p => p - 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Previous</button>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)} className="rounded border bg-white px-3 py-2 disabled:opacity-40">Next</button>
          </div>
        </div>

        {/* Modal */}
        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-3 sm:p-4">
            <div className="max-h-[calc(100dvh-1.5rem)] w-full max-w-md overflow-y-auto overscroll-contain rounded-lg bg-white p-4 shadow-xl sm:max-h-[calc(100dvh-2rem)] sm:p-6">
              <h2 className="text-2xl font-bold text-gray-900 mb-6">
                {editingItem ? 'Edit Food Item' : 'Add Food Item'}
              </h2>

              <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Item Name</label>
                  <input
                    {...register('name', { required: 'Name is required' })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Enter item name"
                  />
                  {errors.name && <p className="text-red-500 text-sm mt-1">{errors.name.message}</p>}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <textarea
                    {...register('description')}
                    rows={2}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="Brief description"
                  />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Food banner image</label>
                  <div className="mb-2 flex items-center gap-3">
                    <SafeImage imagePath={currentImagePath} alt="Food banner preview" className="aspect-video w-40 rounded-lg" />
                    <label className="cursor-pointer rounded-lg border border-orange-300 px-3 py-2 text-sm font-semibold text-orange-700 hover:bg-orange-50">
                      {uploading ? 'Uploading…' : 'Upload JPEG, PNG, or WebP'}
                      <input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" disabled={uploading} onChange={e => { void handleImageUpload(e.target.files?.[0]); e.currentTarget.value = ''; }} />
                    </label>
                  </div>
                  <input
                    {...register('imagePath', { pattern: { value: /^(|\/images\/[A-Za-z0-9_/-]+\.(jpg|jpeg|png|webp)|\/uploads\/foods\/[a-f0-9-]+\.(jpg|jpeg|png|webp))$/i, message: 'Use a project image or upload an image' } })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                    placeholder="/images/burger.jpg or upload an image"
                  />
                  <p className="mt-1 text-xs text-gray-500">Use a food-specific 16:9 image. It is saved to this food item and appears in its menu banner.</p>
                  {errors.imagePath && <p className="text-red-500 text-sm mt-1">{errors.imagePath.message}</p>}
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Price (₹)</label>
                    <input
                      {...register('price', { required: 'Price is required', valueAsNumber: true, min: { value: 0.01, message: 'Price must be greater than 0' } })}
                      type="number"
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                      placeholder="0"
                    />
                    {errors.price && <p className="text-red-500 text-sm mt-1">{errors.price.message}</p>}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                    <input
                      {...register('category', { required: 'Category is required' })}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                      placeholder="e.g., Main Course"
                    />
                    {errors.category && <p className="text-red-500 text-sm mt-1">{errors.category.message}</p>}
                  </div>
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    {...register('available')}
                    type="checkbox"
                    id="available"
                    className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  />
                  <label htmlFor="available" className="text-sm text-gray-700">Available for ordering</label>
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
                    disabled={isSubmitting || uploading}
                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
                  >
                    {isSubmitting ? 'Saving…' : editingItem ? 'Update' : 'Create'}
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

import { useState, useEffect } from 'react';
import { useParams, Navigate } from 'react-router-dom';
import { restaurantApi } from '../api/restaurantApi';
import { cartApi } from '../api/cartApi';
import { Restaurant, FoodItem, CartRequest } from '../types';
import { toast } from 'sonner';
import { MapPin, Star, Utensils } from 'lucide-react';
import SafeImage from '../components/SafeImage';
import FoodItemBanner from '../components/FoodItemBanner';
import FavoriteButton from '../components/FavoriteButton';
import { useWishlist } from '../hooks/useWishlist';

export default function RestaurantDetails() {
  const { id } = useParams<{ id: string }>();
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [loading, setLoading] = useState(true);
  const [addingToCart, setAddingToCart] = useState<{ [key: number]: boolean }>({});
  const [menuCategory, setMenuCategory] = useState<string>('All');
  const { ids: favouriteIds, loading: favouritesLoading, toggle: toggleFavourite } = useWishlist();

  useEffect(() => {
    const fetchRestaurant = async () => {
      if (!id) return;
      try {
        const response = await restaurantApi.getById(Number(id));
        const detailsResponse = await restaurantApi.getMenuItems(Number(id));
        setRestaurant({ ...response.data, foodItems: detailsResponse.data });
      } catch (error) {
        toast.error('Failed to load restaurant details');
      } finally {
        setLoading(false);
      }
    };

    fetchRestaurant();
  }, [id]);

  const handleAddToCart = async (foodItem: FoodItem) => {
    setAddingToCart(prev => ({ ...prev, [foodItem.id]: true }));
    try {
      const cartRequest: CartRequest = {
        foodItemId: foodItem.id,
        quantity: 1,
      };
      await cartApi.addItem(cartRequest);
      toast.success(`${foodItem.name} added to cart!`);
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || 'Failed to add item to cart';
      toast.error(message);
    } finally {
      setAddingToCart(prev => ({ ...prev, [foodItem.id]: false }));
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-gray-200 h-64 rounded-lg animate-pulse mb-8" />
      </div>
    );
  }

  if (!restaurant) {
    return <Navigate to="/restaurants" replace />;
  }

  // Get unique categories from food items
  const categories = ['All', ...Array.from(new Set(restaurant.foodItems?.map(item => item.category) || []))];

  // Filter food items by category
  const filteredItems = restaurant.foodItems?.filter(item =>
    menuCategory === 'All' || item.category === menuCategory
  ) || [];

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      {/* Restaurant Header */}
      <div className="bg-white rounded-lg shadow-md p-6 mb-8">
        <div className="flex flex-col md:flex-row items-start gap-6">
          {/* Image Section */}
          <div className="w-full md:w-1/2 lg:w-1/3">
            <div className="relative h-48 w-full rounded-lg overflow-hidden">
              <SafeImage imagePath={restaurant.bannerImagePath || restaurant.imagePath} alt={`${restaurant.name} restaurant`} kind="restaurant" className="h-full w-full" />
              {/* Rating Badge */}
              <div className="absolute top-3 left-3 bg-yellow-400/90 rounded-full px-2 py-1 text-xs flex items-center space-x-1">
                <Star size={12} className="text-yellow-800" />
                <span className="font-medium">{restaurant.rating?.toFixed(1) || '4.5'}</span>
              </div>
              {/* Time Badge */}
              <div className="absolute top-3 right-3 bg-primary/90 rounded-full px-2 py-1 text-xs text-white flex items-center space-x-1">
                <MapPin size={12} className="text-white" />
                <span className="font-medium">${restaurant.deliveryTime || '30-40'} min</span>
              </div>
              {/* Offer Badge - Only if backend provides offer data */}
              {/* {restaurant.offer && (
                <div className="absolute bottom-3 left-3 bg-primary/90 text-white text-xs px-2 py-0.5 rounded">
                  {restaurant.offer}%
                </div>
              )} */}
            </div>
          </div>

          {/* Info Section */}
          <div className="flex-1 space-y-4">
            <h1 className="text-3xl font-bold text-gray-900">{restaurant.name}</h1>
            <div className="flex items-center space-x-4 text-gray-600">
              <span>⭐</span>
              <span className="font-medium">{restaurant.rating?.toFixed(1) || '4.5'}</span>
              <span>•</span>
              <span>${restaurant.deliveryTime || '30-40'} min</span>
              <span>•</span>
              <span>₹₹</span>
            </div>
            <p className="text-gray-600">{restaurant.description}</p>
            <div className="flex flex-wrap gap-4 mb-4">
              <span className="bg-primary/10 text-primary px-3 py-1 rounded text-sm font-medium">
                {restaurant.cuisineType}
              </span>
              {/* Add more badges if needed */}
            </div>
          </div>
        </div>
      </div>

      {/* Menu Categories */}
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Menu</h2>
        <div className="flex flex-wrap gap-2">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setMenuCategory(category)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-all
                ${menuCategory === category
                  ? 'bg-primary text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'}`}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      {/* Menu Items Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredItems.map((item) => (
          <div key={item.id} className="group bg-white rounded-lg shadow-lg overflow-hidden hover:shadow-xl transition-transform hover:scale-[1.02]">
            <div className="relative">
              <FoodItemBanner item={item} />
              <FavoriteButton saved={favouriteIds.has(item.id)} disabled={favouritesLoading} onClick={() => void toggleFavourite(item.id)} />
            </div>

            {/* Content Section */}
            <div className="p-5">
              <div className="flex items-center justify-between mb-4">
                <span className="text-2xl font-bold text-primary">₹{item.price}</span>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => handleAddToCart(item)}
                    disabled={addingToCart[item.id]}
                    className={`flex items-center space-x-1 bg-primary/10 text-primary px-3 py-2 rounded-lg transition-all hover:bg-primary/20
                      ${addingToCart[item.id] ? 'bg-primary text-white' : ''}`}
                  >
                    {addingToCart[item.id] ? (
                      <>
                        <Utensils size={16} className="mr-1" />
                        Adding...
                      </>
                    ) : (
                      <>
                        <Utensils size={16} className="mr-1" />
                        Add
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Empty State */}
      {filteredItems.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-600 text-lg">No items available in this category</p>
        </div>
      )}
    </div>
  );
}

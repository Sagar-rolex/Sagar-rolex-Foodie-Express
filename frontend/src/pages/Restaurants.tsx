import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { restaurantApi } from '../api/restaurantApi';
import { Restaurant } from '../types';
import { toast } from 'sonner';
import { Search, MapPin, Star } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function Restaurants() {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [filteredRestaurants, setFilteredRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCuisine, setSelectedCuisine] = useState<string>('');
  const [sortOption, setSortOption] = useState('');

  useEffect(() => {
    const fetchRestaurants = async () => {
      try {
        const response = await restaurantApi.getAll();
        const data = Array.isArray(response.data) ? response.data : [];
        setRestaurants(data);
        setFilteredRestaurants(data);
      } catch (error) {
        toast.error('Failed to load restaurants');
      } finally {
        setLoading(false);
      }
    };

    fetchRestaurants();
  }, []);

  useEffect(() => {
    let filtered = restaurants;

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(r =>
        r.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        r.cuisineType.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Cuisine filter
    if (selectedCuisine) {
      filtered = filtered.filter(r => r.cuisineType === selectedCuisine);
    }

    // Sort
    if (sortOption === 'rating-high') {
      filtered = [...filtered].sort((a, b) => (b.rating || 0) - (a.rating || 0));
    } else if (sortOption === 'rating-low') {
      filtered = [...filtered].sort((a, b) => (a.rating || 0) - (b.rating || 0));
    } else if (sortOption === 'time-asc') {
      filtered = [...filtered].sort((a, b) => (a.deliveryTime || 0) - (b.deliveryTime || 0));
    } else if (sortOption === 'time-desc') {
      filtered = [...filtered].sort((a, b) => (b.deliveryTime || 0) - (a.deliveryTime || 0));
    }

    setFilteredRestaurants(filtered);
  }, [searchTerm, selectedCuisine, sortOption, restaurants]);

  const cuisines = Array.from(new Set(restaurants.map(r => r.cuisineType)));

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="mb-6 flex flex-col items-start justify-between gap-2 sm:flex-row sm:items-center">
        <h1 className="text-2xl font-bold">Discover restaurants</h1>
        <div className="text-sm text-gray-500">
          {filteredRestaurants.length} {filteredRestaurants.length === 1 ? 'restaurant' : 'restaurants'} available
        </div>
      </div>

      {/* Search and Filter */}
      <div className="bg-white p-6 rounded-lg shadow-md mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-3 text-gray-400" size={20} />
            <input
              type="text"
              placeholder="🔍 Search restaurants or cuisines"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
            />
          </div>

          {/* Cuisine Filter */}
          <select
            value={selectedCuisine}
            onChange={(e) => setSelectedCuisine(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
          >
            <option value="">All Cuisines</option>
            {cuisines.map(cuisine => (
              <option key={cuisine} value={cuisine}>{cuisine}</option>
            ))}
          </select>

          {/* Sort */}
          <select
            value={sortOption}
            onChange={(e) => setSortOption(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
          >
            <option value="">Sort</option>
            <option value="rating-high">Rating (High to Low)</option>
            <option value="rating-low">Rating (Low to High)</option>
            <option value="time-asc">Delivery Time (Fastest First)</option>
            <option value="time-desc">Delivery Time (Slowest First)</option>
          </select>

          {/* Clear Filters */}
          <button
            onClick={() => {
              setSearchTerm('');
              setSelectedCuisine('');
              setSortOption('');
            }}
            className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition"
          >
            Clear Filters
          </button>
        </div>
      </div>

      {/* Restaurants Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-gray-200 rounded-lg h-96 animate-pulse" />
          ))}
        </div>
      ) : filteredRestaurants.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-600 text-lg">No restaurants found matching your criteria</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredRestaurants.map((restaurant) => (
            <Link key={restaurant.id} to={`/restaurants/${restaurant.id}`}>
              <div className="group bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-transform hover:scale-[1.02]">
                {/* Image with badges */}
                <div className="relative h-48">
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
                {/* Content */}
                <div className="p-4">
                  <h3 className="text-lg font-bold text-gray-900 mb-1">{restaurant.name}</h3>
                  <p className="text-sm text-gray-500 mb-2">{restaurant.cuisineType} • {restaurant.address}</p>
                  <div className="flex items-center justify-between text-sm text-gray-600">
                    <span>₹₹</span>
                    {/* Price indicator */}
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

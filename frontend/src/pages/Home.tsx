import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { restaurantApi } from '../api/restaurantApi';
import { Restaurant } from '../types';
import { toast } from 'sonner';
import { MapPin, Star, Search, ArrowRight, ShieldCheck, Clock } from 'lucide-react';
import SafeImage from '../components/SafeImage';

export default function Home() {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const fetchRestaurants = async () => {
      try {
        const response = await restaurantApi.getAll();
        setRestaurants(Array.isArray(response.data) ? response.data : []);
      } catch (error) {
        toast.error('Failed to load restaurants');
      } finally {
        setLoading(false);
      }
    };

    fetchRestaurants();
  }, []);

  const cuisines = [
    { name: 'North Indian', icon: '🍛' },
    { name: 'South Indian', icon: ' dosa' },
    { name: 'Chinese', icon: '🥡' },
    { name: 'Biryani', icon: '🥘' },
    { name: 'Pizza', icon: '🍕' },
    { name: 'Burgers', icon: '🍔' },
    { name: 'Street Food', icon: '🌮' },
    { name: 'Desserts', icon: '🍰' },
  ];
  const cuisineImages: Record<string, string> = {
    'North Indian': '/images/curry.jpg', 'South Indian': '/images/dosa.jpg', Chinese: '/images/category-chinese.webp',
    Biryani: '/images/category-biryani.webp', Pizza: '/images/pizza.jpg', Burgers: '/images/burger.jpg',
    'Street Food': '/images/category-street-food.webp', Desserts: '/images/category-desserts.webp',
  };

  return (
    <div className="min-h-screen bg-[#FFF9F5] text-[#242424]">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-12 pb-20 md:pt-20 md:pb-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            {/* Left Column: Heading and Search */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center space-x-2 bg-orange-100 text-[#F05A28] px-4 py-1.5 rounded-full text-sm font-semibold">
                <span>⚡</span>
                <span>Superfast Delivery to Your Doorstep</span>
              </div>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#242424] leading-[1.15]">
                Quick and Tasty Food <br className="hidden sm:inline" />
                Delivered with a <span className="text-[#F05A28]">Dash of Speed</span>
              </h1>
              <p className="text-lg text-gray-600 max-w-xl">
                Discover your favorites, explore new flavors, and get delicious, piping hot food delivered right to your door in minutes.
              </p>

              {/* Search Bar */}
              <div className="bg-white p-2 rounded-2xl shadow-xl border border-orange-100 flex flex-col sm:flex-row items-center gap-2 max-w-xl">
                <div className="flex items-center space-x-2 px-3 py-2 w-full sm:w-auto flex-1 border-b sm:border-b-0 sm:border-r border-gray-100">
                  <MapPin className="text-[#F05A28]" size={20} />
                  <input
                    type="text"
                    placeholder="Delivery Location (e.g. Downtown)"
                    className="w-full focus:outline-none text-sm text-gray-800 placeholder-gray-400 bg-transparent"
                    defaultValue="Current Location"
                  />
                </div>
                <div className="flex items-center space-x-2 px-3 py-2 w-full flex-1">
                  <Search className="text-gray-400" size={20} />
                  <input
                    type="text"
                    placeholder="Search dishes, restaurants..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full focus:outline-none text-sm text-gray-800 placeholder-gray-400 bg-transparent"
                  />
                </div>
                <Link
                  to={searchTerm ? `/restaurants?search=${encodeURIComponent(searchTerm)}` : '/restaurants'}
                  className="w-full sm:w-auto bg-[#F05A28] text-white px-8 py-3.5 rounded-xl font-semibold hover:bg-orange-600 transition shadow-lg shadow-orange-500/25 flex items-center justify-center space-x-2"
                >
                  <span>Order Now</span>
                  <ArrowRight size={18} />
                </Link>
              </div>

              {/* Quick Actions / Trust badges */}
              <div className="flex items-center space-x-6 pt-4 text-sm text-gray-500 font-medium">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="text-emerald-600" size={18} />
                  <span>100% Hygienic</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Clock className="text-[#F05A28]" size={18} />
                  <span>Lightning Fast</span>
                </div>
                <div className="flex items-center space-x-2">
                  <Star className="text-amber-500 fill-amber-500" size={18} />
                  <span>Top Rated</span>
                </div>
              </div>
            </div>

            {/* Right Column: Platter / Food Collage */}
            <div className="lg:col-span-5 relative">
              <div className="relative mx-auto max-w-md lg:max-w-none">
                {/* Decorative background blob */}
                <div className="absolute -inset-4 bg-gradient-to-tr from-orange-200 to-amber-100 rounded-3xl transform rotate-3 filter blur-lg opacity-70"></div>

                <div className="relative bg-white p-4 rounded-3xl shadow-2xl border border-orange-100 overflow-hidden group">
                  <SafeImage imagePath="/images/home-signature-biryani.png" alt="A signature Hyderabadi dum biryani topped with saffron rice and herbs" loading="eager" fetchPriority="high" className="h-80 w-full rounded-2xl transition duration-500 group-hover:scale-105 sm:h-96" />
                  <div className="absolute bottom-6 left-6 right-6 bg-white/95 backdrop-blur-md p-4 rounded-2xl shadow-lg border border-gray-100 flex items-center justify-between">
                    <div>
                      <p className="text-xs text-gray-500 font-medium uppercase tracking-wider">Signature Dish</p>
                      <h4 className="font-bold text-gray-900 text-base">Hyderabadi Dum Biryani</h4>
                      <p className="text-xs text-[#F05A28] font-semibold mt-0.5">Saffron rice, slow-cooked chicken & aromatic spices</p>
                    </div>
                    <Link
                      to="/restaurants"
                      className="bg-[#F05A28] text-white p-3 rounded-xl hover:bg-orange-600 transition shadow-md"
                    >
                      <ArrowRight size={20} />
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Cuisine Explorer Section */}
      <section className="py-16 bg-white border-t border-orange-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-end mb-10">
            <div>
              <span className="text-[#F05A28] font-bold text-sm uppercase tracking-wider">Categories</span>
              <h2 className="text-3xl font-bold text-gray-900 mt-1">What are you craving?</h2>
            </div>
            <Link to="/restaurants" className="text-[#F05A28] font-semibold hover:underline flex items-center space-x-1">
              <span>View all</span>
              <ArrowRight size={16} />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-4">
            {cuisines.map((cuisine, index) => (
              <Link
                key={index}
                to={`/restaurants?cuisine=${encodeURIComponent(cuisine.name)}`}
                className="group bg-[#FFF9F5] p-6 rounded-2xl border border-orange-100 text-center hover:bg-[#F05A28] hover:text-white transition-all duration-300 shadow-sm hover:shadow-xl hover:-translate-y-1 flex flex-col items-center justify-center space-y-3"
              >
                <SafeImage imagePath={cuisineImages[cuisine.name]} alt={`${cuisine.name} dishes`} className="h-16 w-16 rounded-full shadow-sm transition duration-300 group-hover:scale-110" />
                <h3 className="font-semibold text-sm text-gray-800 group-hover:text-white transition">
                  {cuisine.name}
                </h3>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Restaurant Discovery Section */}
      <section className="py-16 bg-[#FFF9F5]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-end mb-10">
            <div>
              <span className="text-[#F05A28] font-bold text-sm uppercase tracking-wider">Top Rated</span>
              <h2 className="text-3xl font-bold text-gray-900 mt-1">Restaurants You'll Love</h2>
            </div>
            <Link to="/restaurants" className="text-[#F05A28] font-semibold hover:underline flex items-center space-x-1">
              <span>Explore all</span>
              <ArrowRight size={16} />
            </Link>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="bg-white rounded-2xl h-80 animate-pulse shadow-sm" />
              ))}
            </div>
          ) : restaurants.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl shadow-sm">
              <p className="text-gray-600 text-lg">No restaurants available right now.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
              {restaurants.slice(0, 6).map((restaurant) => (
                <Link key={restaurant.id} to={`/restaurants/${restaurant.id}`} className="group">
                  <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-xl transition-all duration-300 hover:-translate-y-1">
                    <div className="relative h-52 overflow-hidden">
                      <SafeImage imagePath={restaurant.bannerImagePath || restaurant.imagePath} alt={`${restaurant.name} restaurant`} kind="restaurant" className="h-full w-full transition-transform duration-500 group-hover:scale-105" />
                      <div className="absolute top-4 left-4 bg-white/90 backdrop-blur-md rounded-full px-3 py-1 text-xs font-bold text-gray-800 flex items-center space-x-1 shadow-sm">
                        <Star size={14} className="text-amber-500 fill-amber-500" />
                        <span>{restaurant.rating?.toFixed(1) || '4.6'}</span>
                      </div>
                      <div className="absolute top-4 right-4 bg-[#F05A28] text-white rounded-full px-3 py-1 text-xs font-semibold shadow-sm flex items-center space-x-1">
                        <Clock size={12} />
                        <span>{restaurant.deliveryTime || '30-40'} mins</span>
                      </div>
                    </div>
                    <div className="p-6">
                      <h3 className="text-xl font-bold text-gray-900 group-hover:text-[#F05A28] transition">
                        {restaurant.name}
                      </h3>
                      <p className="text-sm text-gray-500 mt-1">
                        {restaurant.cuisineType} • {restaurant.address}
                      </p>
                      <div className="mt-4 pt-4 border-t border-gray-100 flex items-center justify-between text-sm">
                        <span className="text-gray-600 font-medium">₹₹ • Free Delivery</span>
                        <span className="text-[#F05A28] font-bold flex items-center space-x-1 group-hover:translate-x-1 transition">
                          <span>View Menu</span>
                          <ArrowRight size={16} />
                        </span>
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Brand Features Section */}
      <section className="py-20 bg-white border-t border-orange-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <span className="text-[#F05A28] font-bold text-sm uppercase tracking-wider">Why Foodie Express</span>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-gray-900 mt-2 mb-12">
            Good Food. Great Moments.
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-[#FFF9F5] p-8 rounded-3xl border border-orange-100 text-center">
              <div className="w-16 h-16 bg-orange-100 text-[#F05A28] rounded-2xl flex items-center justify-center text-3xl mx-auto mb-6 shadow-inner">
                🌮
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Diverse Cuisines</h3>
              <p className="text-gray-600 text-sm leading-relaxed">
                Explore hundreds of local restaurants and diverse menus in one convenient platform.
              </p>
            </div>

            <div className="bg-[#FFF9F5] p-8 rounded-3xl border border-orange-100 text-center">
              <div className="w-16 h-16 bg-orange-100 text-[#F05A28] rounded-2xl flex items-center justify-center text-3xl mx-auto mb-6 shadow-inner">
                ⚡
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Real-Time Tracking</h3>
              <p className="text-gray-600 text-sm leading-relaxed">
                Follow your order every step of the way, from kitchen preparation straight to your door.
              </p>
            </div>

            <div className="bg-[#FFF9F5] p-8 rounded-3xl border border-orange-100 text-center">
              <div className="w-16 h-16 bg-orange-100 text-[#F05A28] rounded-2xl flex items-center justify-center text-3xl mx-auto mb-6 shadow-inner">
                🛡️
              </div>
              <h3 className="text-xl font-bold text-gray-900 mb-2">Secure Payments</h3>
              <p className="text-gray-600 text-sm leading-relaxed">
                Enjoy 100% secure online checkout via Razorpay with encrypted transactions and instant confirmation.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Promotional Banner */}
      <section className="py-16 bg-[#FFF9F5] px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto bg-gradient-to-r from-[#F05A28] to-amber-600 rounded-3xl p-8 sm:p-12 text-white shadow-xl relative overflow-hidden">
          <div className="absolute right-0 top-0 bottom-0 w-1/2 opacity-20 pointer-events-none hidden lg:block">
            <SafeImage imagePath="/images/shared-meal.jpg" alt="A table with freshly prepared food" className="h-full w-full" />
          </div>
          <div className="relative z-10 max-w-xl">
            <span className="bg-white/20 text-white px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wider">
              Special Experience
            </span>
            <h2 className="text-3xl sm:text-4xl font-extrabold mt-4 mb-4">
              Something Delicious Is Waiting For You.
            </h2>
            <p className="text-white/90 mb-8 text-lg">
              Satisfy your cravings right now with top-tier restaurants and lightning-fast delivery.
            </p>
            <Link
              to="/restaurants"
              className="inline-flex items-center space-x-2 bg-white text-[#F05A28] px-8 py-4 rounded-xl font-bold hover:bg-orange-50 transition shadow-lg"
            >
              <span>Explore Restaurants</span>
              <ArrowRight size=
{18} />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}

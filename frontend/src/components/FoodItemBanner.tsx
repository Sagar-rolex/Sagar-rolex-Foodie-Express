import SafeImage from './SafeImage';
import type { FoodItem } from '../types';

type FoodItemBannerProps = {
  item: FoodItem;
  className?: string;
};

const THEMES: Record<string, { wash: string; accent: string; label: string }> = {
  biryani: { wash: 'from-amber-950/90 via-orange-950/55', accent: 'text-amber-200', label: 'AROMATIC FAVORITE' },
  pizza: { wash: 'from-red-950/90 via-orange-950/45', accent: 'text-orange-200', label: 'OVEN FRESH' },
  burger: { wash: 'from-stone-950/90 via-amber-950/45', accent: 'text-yellow-200', label: 'GRILL HOUSE' },
  dessert: { wash: 'from-fuchsia-950/85 via-rose-950/40', accent: 'text-pink-200', label: 'SWEET MOMENT' },
  beverage: { wash: 'from-cyan-950/85 via-blue-950/45', accent: 'text-cyan-100', label: 'FRESHLY POURED' },
  south: { wash: 'from-emerald-950/85 via-lime-950/45', accent: 'text-lime-100', label: 'SOUTHERN CLASSIC' },
  chinese: { wash: 'from-indigo-950/90 via-red-950/45', accent: 'text-orange-200', label: 'WOK TOSSED' },
  street: { wash: 'from-orange-950/90 via-rose-950/45', accent: 'text-amber-100', label: 'STREET FOOD FAVORITE' },
  classic: { wash: 'from-slate-950/85 via-stone-950/45', accent: 'text-orange-100', label: 'FOODIE EXPRESS PICK' },
};

const BURGER_BARN_IMAGES: Record<string, string> = {
  'Classic Veg Burger': '/images/food-banners/burger-barn-classic-veg-burger.webp',
  'Crispy Chicken Burger': '/images/food-banners/burger-barn-crispy-chicken-burger.webp',
  'Paneer Burger': '/images/food-banners/burger-barn-paneer-burger.webp',
  'Cheese Burger': '/images/food-banners/burger-barn-cheese-burger.webp',
  'Margherita Pizza': '/images/food-banners/burger-barn-margherita-pizza.webp',
  'Farmhouse Pizza': '/images/food-banners/burger-barn-farmhouse-pizza.webp',
  'Paneer Tikka Pizza': '/images/food-banners/burger-barn-paneer-tikka-pizza.webp',
  'Veggie Supreme Pizza': '/images/food-banners/burger-barn-veggie-supreme-pizza.webp',
  'Club Sandwich': '/images/food-banners/burger-barn-club-sandwich.webp',
  'Grilled Cheese Sandwich': '/images/food-banners/burger-barn-grilled-cheese-sandwich.webp',
  'Bombay Sandwich': '/images/food-banners/burger-barn-bombay-sandwich.webp',
  'Garlic Bread': '/images/food-banners/burger-barn-garlic-bread.webp',
  'Cheesy Garlic Bread': '/images/food-banners/burger-barn-cheesy-garlic-bread.webp',
  'Loaded Fries': '/images/food-banners/burger-barn-loaded-fries.webp',
  'Peri-Peri Fries': '/images/food-banners/burger-barn-peri-peri-fries.webp',
  'Cold Coffee': '/images/food-banners/burger-barn-cold-coffee.webp',
  'Chocolate Milkshake': '/images/food-banners/burger-barn-chocolate-milkshake.webp',
};

const RESTAURANT_TWO_IMAGES: Record<string, string> = {
  'Chicken Ramen': '/images/food-banners/restaurant-2-chicken-ramen.webp',
  'Miso Soup': '/images/food-banners/restaurant-2-miso-soup.webp',
  'California Roll': '/images/food-banners/restaurant-2-california-roll.webp',
  Paneer: '/images/food-banners/restaurant-2-paneer.webp',
};

const DAKSHIN_RASOI_IMAGES: Record<string, string> = {
  'Masala Dosa': '/images/food-banners/dakshin-rasoi-masala-dosa.webp',
  'Mysore Masala Dosa': '/images/food-banners/dakshin-rasoi-mysore-masala-dosa.webp',
  'Plain Dosa': '/images/food-banners/dakshin-rasoi-plain-dosa.webp',
  'Rava Dosa': '/images/food-banners/dakshin-rasoi-rava-dosa.webp',
  'Set Dosa': '/images/food-banners/dakshin-rasoi-set-dosa.webp',
  'Onion Uttapam': '/images/food-banners/dakshin-rasoi-onion-uttapam.webp',
  'Idli Sambar (3 pcs)': '/images/food-banners/dakshin-rasoi-idli-sambar-3-pcs.webp',
  'Medu Vada (2 pcs)': '/images/food-banners/dakshin-rasoi-medu-vada-2-pcs.webp',
  'Idli Vada Combo': '/images/food-banners/dakshin-rasoi-idli-vada-combo.webp',
  'Podi Idli': '/images/food-banners/dakshin-rasoi-podi-idli.webp',
  Pongal: '/images/food-banners/dakshin-rasoi-pongal.webp',
  'Lemon Rice': '/images/food-banners/dakshin-rasoi-lemon-rice.webp',
  'Curd Rice': '/images/food-banners/dakshin-rasoi-curd-rice.webp',
  'Appam with Vegetable Stew': '/images/food-banners/dakshin-rasoi-appam-with-vegetable-stew.webp',
  'Kerala Parotta': '/images/food-banners/dakshin-rasoi-kerala-parotta.webp',
  'Filter Coffee': '/images/food-banners/dakshin-rasoi-filter-coffee.webp',
};

const GENERIC_FOOD_IMAGES = new Set([
  '/images/burger.jpg', '/images/pizza.jpg', '/images/shared-meal.jpg',
  '/images/coffee.jpg', '/images/dessert.jpg', '/images/indian-thali.jpg',
  '/images/noodles.jpg', '/images/sushi.jpg', '/images/curry.jpg', '/images/dosa.jpg',
  '/images/salad.jpg', '/images/taco.jpg',
]);

function getTheme(item: FoodItem) {
  const text = `${item.name} ${item.category}`.toLowerCase();
  if (text.includes('biryani')) return THEMES.biryani;
  if (text.includes('pizza')) return THEMES.pizza;
  if (text.includes('burger')) return THEMES.burger;
  if (/jamun|rasmalai|halwa|kheer|jalebi|rasgulla|dessert|sweet/.test(text)) return THEMES.dessert;
  if (/chai|coffee|lassi|juice|soda|beverage|drink|buttermilk|coconut water|milkshake/.test(text)) return THEMES.beverage;
  if (/dosa|idli|uttapam|pongal|appam|vada|south indian/.test(text)) return THEMES.south;
  if (/noodle|manchurian|momo|momos|chinese|fried rice|spring roll|schezwan|chilli/.test(text)) return THEMES.chinese;
  if (/street|chaat|pav bhaji|samosa|kachori|puri|poha|dhokla|vada pav/.test(text)) return THEMES.street;
  return THEMES.classic;
}

function getTagline(item: FoodItem) {
  const description = item.description?.trim().replace(/[.!?]+$/, '');
  if (description) return description;
  return `A ${item.name.toLowerCase()} favorite, ready to enjoy`;
}

/** A responsive, image-led food promotion. The item record supplies its persistent image path. */
export default function FoodItemBanner({ item, className = '' }: FoodItemBannerProps) {
  const theme = getTheme(item);
  const burgerBarnImage = item.restaurant?.name === 'Burger Barn' ? BURGER_BARN_IMAGES[item.name] : undefined;
  const restaurantTwoImage = item.restaurant?.id === 2 ? RESTAURANT_TWO_IMAGES[item.name] : undefined;
  const dakshinRasoiImage = item.restaurant?.name === 'Dakshin Rasoi' ? DAKSHIN_RASOI_IMAGES[item.name] : undefined;
  const generatedImage = restaurantTwoImage || dakshinRasoiImage || burgerBarnImage;
  const imagePath = generatedImage && (!item.imagePath || GENERIC_FOOD_IMAGES.has(item.imagePath))
    ? generatedImage
    : item.imagePath;
  const composition = item.id % 3;
  const copyPlacement = composition === 1 ? 'items-end text-right' : composition === 2 ? 'items-center text-center' : 'items-start text-left';
  const alignment = composition === 1 ? 'ml-auto' : composition === 2 ? 'mx-auto' : '';

  return (
    <div className={`relative isolate aspect-[16/9] overflow-hidden bg-stone-900 ${className}`}>
      <SafeImage imagePath={imagePath} alt={`${item.name}, prepared food`} className="absolute inset-0 h-full w-full transition-transform duration-700 group-hover:scale-105" />
      <div className={`absolute inset-0 bg-gradient-to-r ${theme.wash} to-transparent`} aria-hidden="true" />
      <div className={`absolute inset-0 flex flex-col justify-between p-4 text-white sm:p-5 ${copyPlacement}`}>
        <span className={`rounded-full border border-white/30 bg-black/20 px-2.5 py-1 text-[9px] font-bold tracking-[0.18em] backdrop-blur-sm sm:text-[10px] ${theme.accent}`}>
          {theme.label}
        </span>
        <div className={`max-w-[88%] ${alignment}`}>
          <p className="mb-1 text-[9px] font-semibold uppercase tracking-[0.2em] text-white/80 sm:text-[10px]">Foodie Express</p>
          <h3 className="text-xl font-extrabold leading-tight drop-shadow sm:text-2xl">{item.name}</h3>
          <p className="mt-1 line-clamp-2 text-xs leading-snug text-white/90 drop-shadow sm:text-sm">{getTagline(item)}</p>
        </div>
      </div>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { Store, Utensils } from 'lucide-react';

type SafeImageProps = {
  imagePath?: string | null;
  alt: string;
  kind?: 'food' | 'restaurant';
  className: string;
  loading?: 'lazy' | 'eager';
  fetchPriority?: 'high' | 'low' | 'auto';
};

const FALLBACKS = {
  food: '/images/indian-thali.jpg',
  restaurant: '/images/restaurant-interior.jpg',
} as const;

function isLocalImagePath(path: string): boolean {
  return (/^\/images\/[A-Za-z0-9_/-]+\.(?:jpg|jpeg|png|webp)$/i.test(path)
    || /^\/uploads\/(?:foods|restaurants)\/[a-fA-F0-9-]+\.(?:jpg|jpeg|png|webp)$/i.test(path)) && !path.includes('..');
}

const API_ROOT = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/api\/?$/i, '');

/** Keeps the image frame painted while loading and swaps to a local photo on any failure. */
export default function SafeImage({ imagePath, alt, kind = 'food', className, loading = 'lazy', fetchPriority = 'auto' }: SafeImageProps) {
  const fallbackPath = FALLBACKS[kind];
  const requestedPath = imagePath && isLocalImagePath(imagePath)
    ? imagePath.startsWith('/uploads/') ? `${API_ROOT}${imagePath}` : imagePath
    : fallbackPath;
  const [src, setSrc] = useState(requestedPath);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setSrc(requestedPath);
    setLoaded(false);
    setFailed(false);
  }, [requestedPath]);

  return (
    <div
      className={`relative overflow-hidden bg-gradient-to-br from-orange-100 via-amber-50 to-orange-200 ${className}`}
      role={failed ? 'img' : undefined}
      aria-label={failed ? alt : undefined}
    >
      {!failed && (
        <img
          src={src}
          alt={alt}
          loading={loading}
          ref={(image) => image?.setAttribute('fetchpriority', fetchPriority)}
          decoding="async"
          onLoad={() => setLoaded(true)}
          onError={() => {
            if (src !== fallbackPath) {
              setSrc(fallbackPath);
              setLoaded(false);
            } else {
              setFailed(true);
            }
          }}
          className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-300 ${loaded ? 'opacity-100' : 'opacity-0'}`}
        />
      )}
      {(!loaded || failed) && (
        <div className="absolute inset-0 flex items-center justify-center" aria-hidden="true">
          <div className="flex flex-col items-center gap-1 text-orange-700/70">
            {kind === 'restaurant' ? <Store size={30} /> : <Utensils size={28} />}
            <span className="text-[10px] font-semibold tracking-wide">FOODIE EXPRESS</span>
          </div>
        </div>
      )}
    </div>
  );
}

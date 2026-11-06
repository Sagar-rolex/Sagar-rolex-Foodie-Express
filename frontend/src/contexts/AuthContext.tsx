import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { LoginResponse } from '../types';
import { cartApi } from '../api/cartApi';
import { toast } from 'sonner';

interface AuthContextType {
  isAuthenticated: boolean;
  user: LoginResponse | null;
  isAdmin: boolean;
  userRole: string | null;
  sessionToken: string | null;
  loading: boolean;
  login: (userData: LoginResponse) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<LoginResponse | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('sessionToken');
    if (storedUser && storedToken) {
      try {
        setUser(JSON.parse(storedUser));
        setIsAuthenticated(true);
      } catch {
        localStorage.removeItem('user');
        localStorage.removeItem('sessionToken');
      }
    }
    setLoading(false);
    const handleExpired = () => {
      setUser(null);
      setIsAuthenticated(false);
      localStorage.removeItem('user');
      localStorage.removeItem('sessionToken');
      window.dispatchEvent(new Event('cart:updated'));
    };
    window.addEventListener('auth:expired', handleExpired);
    return () => window.removeEventListener('auth:expired', handleExpired);
  }, []);

  const login = async (userData: LoginResponse) => {
    setUser(userData);
    setIsAuthenticated(true);
    localStorage.setItem('user', JSON.stringify(userData));
    localStorage.setItem('sessionToken', userData.sessionToken);
    if (userData.role === 'CUSTOMER') {
      try {
        const merged = await cartApi.mergeGuestCart();
        if (merged?.skippedFoodItemIds.length) {
          toast.warning('Some saved cart items were unavailable or from another restaurant. Review your cart before checkout.');
        }
        if (merged?.priceChangedFoodItemIds.length) {
          toast.info('A saved dish changed price. Review the updated cart total before checkout.');
        }
      } catch {
        toast.error('You are signed in, but your guest cart could not be merged. It is still saved; open your cart to retry.');
      }
    }
    window.dispatchEvent(new Event('cart:updated'));
  };

  const logout = () => {
    setUser(null);
    setIsAuthenticated(false);
    localStorage.removeItem('user');
    localStorage.removeItem('sessionToken');
    window.dispatchEvent(new Event('cart:updated'));
  };

  const isAdmin = user?.role === 'ADMIN';
  const userRole = user?.role || null;
  const sessionToken = user?.sessionToken || localStorage.getItem('sessionToken');
  return (
    <AuthContext.Provider value={{ isAuthenticated, user, isAdmin, userRole, sessionToken, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};

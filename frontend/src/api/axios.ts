import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosError } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Send JWT as a bearer token; retain the legacy header during the API transition.
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('sessionToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
      config.headers['X-Session-Token'] = token;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle 401 errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Don't loop redirect if already on login/register endpoints
      const requestUrl = error.config?.url || '';
      const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/register') || requestUrl.includes('/admin/login');
      
      if (!isAuthEndpoint) {
        // Clear invalid session
        localStorage.removeItem('sessionToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:expired'));

        const currentPath = window.location.pathname;
        if (!currentPath.includes('/login')) {
          const redirectPath = currentPath.startsWith('/admin') ? '/admin/login' : '/login';
          window.location.href = `${redirectPath}?expired=true`;
        }
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
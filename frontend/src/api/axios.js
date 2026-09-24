/**
 * Axios Instance with JWT Interceptors
 *
 * PURPOSE:
 * Centralized HTTP client configuration. Every API call goes
 * through this instance, ensuring consistent behavior:
 * - Base URL configuration
 * - Automatic JWT attachment
 * - Automatic 401 handling (expired/invalid tokens)
 *
 * WHY INTERCEPTORS?
 * - REQUEST interceptor: Automatically attaches JWT from localStorage
 *   to every request. No need to manually add headers everywhere.
 * - RESPONSE interceptor: Catches 401 errors globally. If a token
 *   expires mid-session, the user is redirected to login.
 *
 * WHY localStorage?
 * - Simpler than httpOnly cookies for SPA development
 * - Survives page refreshes
 * - For Day 1, this is acceptable. Production could use httpOnly cookies.
 */
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 60000, // 60 second timeout to accommodate Render free tier cold starts
});

let activeRequests = 0;
let sleepTimer = null;

axiosInstance.interceptors.request.use(
  (config) => {
    activeRequests++;
    if (activeRequests === 1) {
      sleepTimer = setTimeout(() => {
        window.dispatchEvent(new CustomEvent('api-slow-request'));
      }, 3000); // 3 seconds means Render is probably waking up
    }

    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    activeRequests--;
    if (activeRequests === 0 && sleepTimer) {
      clearTimeout(sleepTimer);
      window.dispatchEvent(new CustomEvent('api-request-completed'));
    }
    return Promise.reject(error);
  }
);

/**
 * RESPONSE INTERCEPTOR
 * Handles 401 errors globally.
 */
axiosInstance.interceptors.response.use(
  (response) => {
    activeRequests--;
    if (activeRequests === 0 && sleepTimer) {
      clearTimeout(sleepTimer);
      window.dispatchEvent(new CustomEvent('api-request-completed'));
    }
    return response;
  },
  (error) => {
    activeRequests--;
    if (activeRequests === 0 && sleepTimer) {
      clearTimeout(sleepTimer);
      window.dispatchEvent(new CustomEvent('api-request-completed'));
    }

    if (error.response?.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/auth/login');
      if (!isLoginRequest) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;

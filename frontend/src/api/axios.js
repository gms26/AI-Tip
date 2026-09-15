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
  timeout: 10000, // 10 second timeout
});

/**
 * REQUEST INTERCEPTOR
 * Attaches the JWT token from localStorage to every outgoing request.
 *
 * WHY check for token existence?
 * Public endpoints (register, login, health) don't need a token.
 * If no token exists, the request goes out without Authorization header.
 */
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * RESPONSE INTERCEPTOR
 * Handles 401 errors globally.
 *
 * WHY redirect on 401?
 * A 401 means the token is expired, invalid, or missing.
 * The user needs to re-authenticate. Clearing localStorage
 * and redirecting prevents infinite retry loops.
 *
 * WHY check for login path?
 * Don't redirect if the 401 came from the login endpoint itself.
 * That's a "bad credentials" error, not an expired token.
 */
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
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

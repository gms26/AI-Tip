/**
 * Authentication Context — Global Auth State Management
 *
 * PURPOSE:
 * Provides authentication state and functions to the entire
 * React component tree. Any component can access user info,
 * login/logout functions, and auth status.
 *
 * WHY React Context?
 * - Auth state is needed by many components (navbar, dashboard, routes)
 * - Prop drilling would be tedious and fragile
 * - Context provides clean, centralized state management
 * - For Day 1 scope, Context is simpler than Redux/Zustand
 *
 * WHY localStorage?
 * - JWT survives page refreshes and tab closures
 * - User doesn't need to re-login after refresh
 * - Token is automatically attached by the axios interceptor
 *
 * STATE:
 * - user: { name, email } or null
 * - token: JWT string or null
 * - loading: true during initial auth check
 */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loginUser, registerUser } from '../api/authApi';
import { getCurrentUser } from '../api/userApi';

const AuthContext = createContext(null);

/**
 * Custom hook for consuming auth context.
 *
 * WHY a custom hook?
 * - Enforces that context is used within a provider
 * - Cleaner than importing AuthContext + useContext everywhere
 * - Provides a descriptive error if used outside provider
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

/**
 * Auth Provider Component — wraps the app to provide auth state.
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  /**
   * On mount: if a token exists in localStorage, validate it
   * by fetching the current user from the backend.
   *
   * WHY validate on mount?
   * The token in localStorage might be expired or invalid.
   * Fetching /api/users/me confirms the token is still valid.
   * If it fails, we clear the stale token.
   */
  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        try {
          const userData = await getCurrentUser();
          setUser({ name: userData.name, email: userData.email });
        } catch {
          // Token is invalid or expired — clear everything
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Login function — authenticates and stores JWT.
   *
   * @param {Object} credentials - { email, password }
   * @returns {Promise} resolves on success
   * @throws {Error} on failure (bad credentials, network error)
   */
  const login = useCallback(async (credentials) => {
    const data = await loginUser(credentials);
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify({ name: data.name, email: data.email }));
    setToken(data.token);
    setUser({ name: data.name, email: data.email });
  }, []);

  /**
   * Register function — creates account and stores JWT.
   *
   * @param {Object} userData - { name, email, password }
   * @returns {Promise} resolves on success
   * @throws {Error} on failure (duplicate email, validation error)
   */
  const register = useCallback(async (userData) => {
    const data = await registerUser(userData);
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify({ name: data.name, email: data.email }));
    setToken(data.token);
    setUser({ name: data.name, email: data.email });
  }, []);

  /**
   * Logout function — clears all auth state.
   *
   * WHY clear both state and localStorage?
   * State controls the current render.
   * localStorage controls the next page load.
   * Both must be cleared to fully log out.
   */
  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!token && !!user,
    login,
    register,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;

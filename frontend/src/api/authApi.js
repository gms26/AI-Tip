/**
 * Authentication API Functions
 *
 * PURPOSE:
 * Encapsulates all authentication-related API calls.
 * Controllers on the backend map to functions here.
 *
 * WHY SEPARATE API FILES?
 * - Single Responsibility: authApi handles auth, userApi handles users
 * - Easy to find and maintain API calls
 * - If the endpoint changes, only one file needs updating
 * - Components never call axios directly
 */
import axiosInstance from './axios';

/**
 * Register a new user.
 *
 * @param {Object} data - { name, email, password }
 * @returns {Promise} - AuthResponse { token, tokenType, name, email }
 */
export const registerUser = async (data) => {
  const response = await axiosInstance.post('/api/auth/register', data);
  return response.data;
};

/**
 * Login an existing user.
 *
 * @param {Object} data - { email, password }
 * @returns {Promise} - AuthResponse { token, tokenType, name, email }
 */
export const loginUser = async (data) => {
  const response = await axiosInstance.post('/api/auth/login', data);
  return response.data;
};

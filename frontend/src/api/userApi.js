/**
 * User API Functions
 *
 * PURPOSE:
 * Encapsulates all user-related API calls.
 * Separated from authApi following SRP.
 *
 * WHY SEPARATE FROM authApi?
 * - Auth endpoints are public (no token needed)
 * - User endpoints are protected (token required)
 * - Different concerns = different modules
 */
import axiosInstance from './axios';

/**
 * Get the currently authenticated user's profile.
 * Requires a valid JWT token (attached automatically by axios interceptor).
 *
 * @returns {Promise} - UserResponse { id, name, email, createdAt }
 */
export const getCurrentUser = async () => {
  const response = await axiosInstance.get('/api/users/me');
  return response.data;
};

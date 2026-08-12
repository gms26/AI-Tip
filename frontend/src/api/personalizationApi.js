import axiosInstance from './axios';

/**
 * Personalization API module
 *
 * PURPOSE:
 * Encapsulates network requests for personalized tipping insights.
 * Uses the pre-configured axiosInstance to automatically attach JWT tokens.
 */
const personalizationApi = {
  /**
   * Get the current user's overall tipping pattern summary.
   */
  getSummary: async () => {
    const response = await axiosInstance.get('/api/personalization/summary');
    return response.data;
  },

  /**
   * Get restaurant-specific personalization for the current user.
   * @param {string} name - The restaurant name
   */
  getRestaurantPersonalization: async (name) => {
    const response = await axiosInstance.get(`/api/personalization/restaurant?name=${encodeURIComponent(name)}`);
    return response.data;
  }
};

export default personalizationApi;

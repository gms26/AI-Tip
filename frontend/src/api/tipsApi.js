import axiosInstance from './axios';

/**
 * Tips API module
 *
 * PURPOSE:
 * Encapsulates all network requests for the Tip Calculator module.
 * Uses the pre-configured axiosInstance which automatically attaches
 * the JWT token and handles 401s.
 */
const tipsApi = {
  /**
   * Save a new tip calculation
   * @param {Object} data - { billAmount, tipPercentage, restaurantName, currency, serviceQuality }
   */
  createTip: async (data) => {
    const response = await axiosInstance.post('/api/tips', data);
    return response.data;
  },

  /**
   * Get paginated tips for the current user
   * @param {number} page - page number (0-indexed)
   * @param {number} size - page size
   */
  getUserTips: async (page = 0, size = 20) => {
    const response = await axiosInstance.get(`/api/tips?page=${page}&size=${size}`);
    return response.data;
  },

  /**
   * Get a single tip by ID
   */
  getTipById: async (id) => {
    const response = await axiosInstance.get(`/api/tips/${id}`);
    return response.data;
  },

  /**
   * Delete a tip by ID
   */
  deleteTip: async (id) => {
    const response = await axiosInstance.delete(`/api/tips/${id}`);
    return response.data;
  },

  /**
   * Perform a stateless split calculation
   * @param {Object} data - { billAmount, tipPercentage, people, customSplit }
   */
  splitTip: async (data) => {
    const response = await axiosInstance.post('/api/tips/split', data);
    return response.data;
  },

  /**
   * Update the service quality rating for an existing tip
   * @param {string} id - The tip UUID
   * @param {string} serviceQuality - One of: POOR, AVERAGE, GOOD, EXCELLENT
   */
  updateServiceQuality: async (id, serviceQuality) => {
    const response = await axiosInstance.patch(`/api/tips/${id}/service-quality`, { serviceQuality });
    return response.data;
  }
};

export default tipsApi;

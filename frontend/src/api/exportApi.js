import axiosInstance from './axios';

/**
 * Export API module
 *
 * PURPOSE:
 * Encapsulates network requests for data exports (Day 15).
 * Uses responseType: 'blob' to handle file downloads.
 */
const exportApi = {
  /**
   * Export tip history
   * @param {Object} filters - { startDate, endDate, restaurantName, currency, serviceQuality }
   * @param {string} format - 'csv' or 'json'
   * @returns {Blob} The downloaded file blob
   */
  exportTips: async (filters, format = 'csv') => {
    const response = await axiosInstance.post(`/api/export/tips?format=${format}`, filters, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Export tip analytics summary
   * @param {Object} request - AnalyticsRequest { period, startDate, endDate, restaurantName, serviceQuality }
   * @returns {Blob} The downloaded file blob
   */
  exportAnalytics: async (request) => {
    const response = await axiosInstance.post('/api/export/analytics', request, {
      responseType: 'blob',
    });
    return response.data;
  }
};

export default exportApi;

import axiosInstance from './axios';

/**
 * AI API module
 *
 * PURPOSE:
 * Encapsulates network requests for AI Tip Suggestions.
 * Uses the pre-configured axiosInstance to automatically attach JWT tokens.
 */
const aiApi = {
  /**
   * Get an AI tip suggestion based on service context.
   * 
   * @param {Object} data - { billAmount, restaurantType, serviceQuality, country, currency, occasion }
   * @returns {Promise<Object>} - The AI recommendation and exact math totals
   */
  getAiSuggestion: async (data) => {
    const response = await axiosInstance.post('/api/ai/suggest', data);
    return response.data;
  }
};

export default aiApi;

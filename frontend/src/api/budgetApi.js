import axiosInstance from './axios';

/**
 * Budget API module
 *
 * PURPOSE:
 * Encapsulates network requests for tip budget management (Day 17).
 */
const budgetApi = {
  /**
   * Get all budgets for the authenticated user.
   * @returns {Array} List of budget objects
   */
  getBudgets: async () => {
    const response = await axiosInstance.get('/api/tip-budgets');
    return response.data;
  },

  /**
   * Create or update a budget.
   * @param {Object} data - { currency, monthlyLimit, warningThreshold }
   * @returns {Object} The saved budget
   */
  upsertBudget: async (data) => {
    const response = await axiosInstance.put('/api/tip-budgets', data);
    return response.data;
  },

  /**
   * Get the deterministic budget status for a currency.
   * @param {string} currency - ISO 4217 currency code
   * @returns {Object} Budget status response
   */
  getBudgetStatus: async (currency) => {
    const response = await axiosInstance.get(`/api/tip-budgets/${currency}/status`);
    return response.data;
  },

  /**
   * Delete a budget for a currency.
   * @param {string} currency - ISO 4217 currency code
   */
  deleteBudget: async (currency) => {
    await axiosInstance.delete(`/api/tip-budgets/${currency}`);
  }
};

export default budgetApi;

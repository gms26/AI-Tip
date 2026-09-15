import axios from 'axios';

const API_URL = '/api/tip-optimization';

/**
 * Fetches tip optimization insights for the authenticated user.
 *
 * @param {Object} data - Optimization request parameters
 * @param {string} data.currency - ISO 4217 currency code
 * @param {number} data.currentTipPercentage - Current tip percentage (0-100)
 * @param {number} [data.monthlyBudget] - Optional monthly budget assumption
 * @param {number} [data.billAmount] - Optional bill amount for comparison
 * @returns {Promise<Object>} Optimization response with statistics and insights
 */
export const getOptimization = async (data) => {
  const token = localStorage.getItem('token');
  const response = await axios.post(API_URL, data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return response.data;
};

export default { getOptimization };

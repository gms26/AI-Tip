import axios from './axios';

const API_URL = '/api/receipts/reconcile';

/**
 * Checks for matching tip history to reconcile receipts and prevent duplicates.
 *
 * @param {Object} data - Reconciliation request parameters
 * @param {number} data.billAmount - The OCR-extracted bill amount
 * @param {string} data.restaurantName - The OCR-extracted restaurant name
 * @param {string} data.currency - The OCR-extracted currency code
 * @param {string} [data.receiptDate] - Optional receipt date (YYYY-MM-DD)
 * @param {number} [data.tipAmount] - Optional OCR-extracted tip amount
 * @returns {Promise<Object>} Reconciliation match results (NO_MATCH, POSSIBLE_MATCH, STRONG_MATCH)
 */
export const reconcileReceipt = async (data) => {
  const token = localStorage.getItem('token');
  const response = await axios.post(API_URL, data, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return response.data;
};

export default { reconcileReceipt };

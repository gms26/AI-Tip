import axiosInstance from './axios';

/**
 * Currency Conversion API module
 *
 * PURPOSE:
 * Encapsulates network requests for currency conversion operations.
 * Uses the pre-configured axiosInstance to automatically attach JWT tokens.
 */
const currencyApi = {
  /**
   * Fetches the current exchange rate between two currencies.
   * 
   * @param {string} from - Source currency code (e.g. 'USD')
   * @param {string} to - Target currency code (e.g. 'INR')
   * @returns {Promise<Object>} The currency conversion response
   */
  getExchangeRate: async (from, to) => {
    const response = await axiosInstance.get(`/api/currency/rate?from=${from}&to=${to}`);
    return response.data;
  },

  /**
   * Converts a specific amount on the backend.
   * (Available for general use, though the calculator uses getExchangeRate to batch convert)
   */
  convertAmount: async (amount, sourceCurrency, targetCurrency) => {
    const response = await axiosInstance.post('/api/currency/convert', {
      amount,
      sourceCurrency,
      targetCurrency
    });
    return response.data;
  }
};

export default currencyApi;

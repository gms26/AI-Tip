import axiosInstance from './axios';

/**
 * Tip Evolution API
 * 
 * Fetches the user's historical tipping evolution and behavioral timeline.
 */
export const tipEvolutionApi = {
    /**
     * Get the user's tip evolution timeline.
     * @param {string} [currency] - Optional currency to filter by (e.g. 'USD')
     * @param {string} [period] - Optional period (LAST_3_MONTHS, LAST_6_MONTHS, LAST_12_MONTHS, ALL_TIME)
     * @returns {Promise<Object>} The tip evolution response
     */
    getEvolution: async (currency, period) => {
        const params = new URLSearchParams();
        if (currency) {
            params.append('currency', currency);
        }
        if (period) {
            params.append('period', period);
        }
        const query = params.toString();
        const url = `/api/tip-evolution${query ? `?${query}` : ''}`;
        const response = await axiosInstance.get(url);
        return response.data;
    }
};

import axiosInstance from './axios';

/**
 * Tip Profile API
 * 
 * Fetches the user's personalized tipping profile, calculated
 * deterministically based on their entire tip history.
 */
export const tipProfileApi = {
    /**
     * Get the user's tip profile.
     * @param {string} currency - Optional currency to filter by (e.g. 'USD')
     * @returns {Promise<Object>} The tip profile response
     */
    getProfile: async (currency) => {
        const params = new URLSearchParams();
        if (currency) {
            params.append('currency', currency);
        }
        const url = `/api/tip-profile${params.toString() ? `?${params.toString()}` : ''}`;
        const response = await axiosInstance.get(url);
        return response.data;
    }
};

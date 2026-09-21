import axios from './axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-timing' : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/api/tip-timing';

/**
 * Api calls related to tip timing functionality.
 */
const tipTimingApi = {
    /**
     * Gets the tip timing state and confidence based on the current form context.
     * 
     * @param {Object} request - TipTimingRequest object
     * @param {number} request.billAmount
     * @param {number} request.tipPercentage
     * @param {string} request.restaurantName
     * @param {string} request.serviceQuality
     * @param {boolean} request.saved
     * @returns {Promise<Object>} TipTimingResponse
     */
    getTipTiming: async (request) => {
        const token = localStorage.getItem('token');
        const response = await axios.post(BASE_URL, request, {
            headers: {
                Authorization: `Bearer ${token}`
            }
        });
        return response.data;
    }
};

export default tipTimingApi;

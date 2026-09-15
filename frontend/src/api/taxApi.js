import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tax' : 'http://localhost:8080/api/tax';

const taxApi = {
    /**
     * Fetch tax summary based on period and assumptions
     * 
     * @param {Object} data - TaxSummaryRequest
     * @returns {Promise<Object>} TaxSummaryResponse
     */
    getTaxSummary: async (data) => {
        const token = localStorage.getItem('token');
        const response = await axios.post(`${BASE_URL}/summary`, data, {
            headers: {
                Authorization: `Bearer ${token}`
            }
        });
        return response.data;
    }
};

export default taxApi;

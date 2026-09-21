import axios from './axios';

const API_URL = '/api/recommendations';

export const tipRecommendationApi = {
    getRecommendations: async (currency = '') => {
        const url = currency ? `${API_URL}?currency=${encodeURIComponent(currency)}` : API_URL;
        const response = await axios.get(url);
        return response.data;
    }
};

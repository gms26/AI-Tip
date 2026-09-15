import axiosInstance from './axios';

export const tipRecommendationActionApi = {
    performAction: async (type, currency, actionData) => {
        const params = new URLSearchParams();
        if (currency) {
            params.append('currency', currency);
        }
        
        const response = await axiosInstance.post(`/api/recommendations/${type}/action?${params.toString()}`, actionData);
        return response.data;
    },

    getHistory: async () => {
        const response = await axiosInstance.get('/api/recommendations/history');
        return response.data;
    }
};

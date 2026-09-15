import axios from 'axios';

const API_URL = '/api/analytics';

const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
};

const analyticsApi = {
    getTipAnalytics: async (request) => {
        const response = await axios.post(`${API_URL}/tips`, request, getAuthHeaders());
        return response.data;
    }
};

export default analyticsApi;

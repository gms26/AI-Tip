import axios from './axios';

const API_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/payments' : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/api/payments';

const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

const paymentApi = {
    createSession: async (requestData) => {
        const response = await axios.post(`${API_URL}/session`, requestData, getAuthHeaders());
        return response.data;
    },
    getSession: async (sessionId) => {
        const response = await axios.get(`${API_URL}/${sessionId}`, getAuthHeaders());
        return response.data;
    },
    cancelSession: async (sessionId) => {
        const response = await axios.post(`${API_URL}/${sessionId}/cancel`, {}, getAuthHeaders());
        return response.data;
    }
};

export default paymentApi;

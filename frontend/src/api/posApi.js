import axios from 'axios';

const API_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/pos' : 'http://localhost:8080/api/pos';

const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        headers: {
            Authorization: `Bearer ${token}`
        }
    };
};

const posApi = {
    getBill: async (requestData) => {
        const response = await axios.post(`${API_URL}/bill`, requestData, getAuthHeaders());
        return response.data;
    }
};

export default posApi;

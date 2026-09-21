import axios from './axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tips/data-quality' : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/api/tips/data-quality',
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export const dataQualityApi = {
  getDataQuality: async (params = {}) => {
    const response = await api.get('', { params });
    return response.data;
  }
};

export default dataQualityApi;

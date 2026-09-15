import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-forecast' : 'http://localhost:8080/api/tip-forecast',
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

export const tipForecastApi = {
  getForecast: async (params = {}) => {
    const response = await api.get('', { params });
    return response.data;
  }
};

export default tipForecastApi;

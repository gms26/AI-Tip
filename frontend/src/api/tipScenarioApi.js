import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-scenarios' : 'http://localhost:8080/api/tip-scenarios',
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

export const tipScenarioApi = {
  calculateScenarios: async (data) => {
    const response = await api.post('', data);
    return response.data;
  }
};

export default tipScenarioApi;

import axios from './axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tip-goals' : (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080') + '/api/tip-goals',
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

export const goalApi = {
  createGoal: async (data) => {
    const response = await api.post('', data);
    return response.data;
  },

  getGoals: async () => {
    const response = await api.get('');
    return response.data;
  },

  getGoal: async (id) => {
    const response = await api.get(`/${id}`);
    return response.data;
  },

  getGoalProgress: async (id) => {
    const response = await api.get(`/${id}/progress`);
    return response.data;
  },

  getGoalSummary: async () => {
    const response = await api.get('/summary');
    return response.data;
  },

  cancelGoal: async (id) => {
    const response = await api.delete(`/${id}`);
    return response.data;
  }
};

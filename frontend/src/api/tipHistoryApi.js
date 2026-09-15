import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api/tips/history' : 'http://localhost:8080/api/tips/history';

export const searchTipHistory = async (filters) => {
  const token = localStorage.getItem('token');
  if (!token) throw new Error('No authentication token found');

  const response = await axios.post(`${BASE_URL}/search`, filters, {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });

  return response.data;
};

export default {
  searchTipHistory
};

import axios from 'axios';

const API_URL = '/api/insights/tips';

export const getTipInsights = async () => {
  const token = localStorage.getItem('token');
  const response = await axios.get(API_URL, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  return response.data;
};

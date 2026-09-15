import axios from 'axios';

const API_URL = '/api/achievements';

const getHeaders = () => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`,
});

export const getAchievements = async () => {
  const response = await axios.get(API_URL, { headers: getHeaders() });
  return response.data;
};

export const getAchievementSummary = async () => {
  const response = await axios.get(`${API_URL}/summary`, { headers: getHeaders() });
  return response.data;
};

export const evaluateAchievements = async () => {
  const response = await axios.post(`${API_URL}/evaluate`, {}, { headers: getHeaders() });
  return response.data; // Returns list of newly unlocked achievements
};

export default {
  getAchievements,
  getAchievementSummary,
  evaluateAchievements,
};

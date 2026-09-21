import axios from './axios';

const API_URL = '/api/achievements';

export const getAchievements = async () => {
  const response = await axios.get(API_URL);
  return response.data;
};

export const getAchievementSummary = async () => {
  const response = await axios.get(`${API_URL}/summary`);
  return response.data;
};

export const evaluateAchievements = async () => {
  const response = await axios.post(`${API_URL}/evaluate`);
  return response.data; // Returns list of newly unlocked achievements
};

export default {
  getAchievements,
  getAchievementSummary,
  evaluateAchievements,
};

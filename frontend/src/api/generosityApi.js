import axiosInstance from './axios';

/**
 * Generosity Score API
 * 
 * Fetches the deterministic, backend-calculated generosity score based on
 * the user's entire tipping history.
 * 
 * This score is bounded [0-100] and maps to descriptive categories
 * (CONSERVATIVE, MODERATE, GENEROUS, VERY_GENEROUS).
 */
export const getGenerosityScore = async () => {
  const response = await axiosInstance.get('/api/generosity/score');
  return response.data;
};

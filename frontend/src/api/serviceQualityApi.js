import axiosInstance from './axios';

/**
 * Service Quality API module
 *
 * PURPOSE:
 * Encapsulates network requests for the Service Quality Statistics feature.
 * Kept separate from tipsApi because the /api/service-quality domain is
 * distinct — it queries aggregated statistics, not individual tip records.
 * This follows the same pattern as personalizationApi.js and currencyApi.js.
 *
 * SECURITY:
 * All requests include the JWT token automatically via axiosInstance interceptors.
 * User identity is resolved server-side from the token — no userId in requests.
 */
const serviceQualityApi = {
  /**
   * Fetches service quality statistics for the authenticated user.
   *
   * Returns:
   * {
   *   totalRatedTips: number,
   *   poorCount: number,
   *   averageCount: number,
   *   goodCount: number,
   *   excellentCount: number,
   *   mostCommon: "POOR"|"AVERAGE"|"GOOD"|"EXCELLENT"|null,
   *   averageTipPercentageByQuality: { POOR?: number, AVERAGE?: number, GOOD?: number, EXCELLENT?: number }
   * }
   *
   * Empty state: totalRatedTips = 0 — not an error.
   */
  getSummary: async () => {
    const response = await axiosInstance.get('/api/service-quality/summary');
    return response.data;
  }
};

export default serviceQualityApi;

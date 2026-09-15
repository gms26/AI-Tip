import axiosInstance from './axios';

/**
 * Smart Tip Assistant & Feedback API (Day 31 & Day 32)
 * 
 * Fetches context-aware advisory tip suggestions and records explicit
 * user behavioral decisions upon saving tips.
 */
export const smartTipApi = {
    /**
     * Get context-aware smart tip suggestions.
     * @param {Object} data - Request payload
     * @param {number|string} data.billAmount - Bill amount (must be > 0.01)
     * @param {string} data.currency - ISO currency code (e.g. 'USD')
     * @param {string} [data.restaurantName] - Optional restaurant name
     * @param {string} [data.serviceQuality] - Optional service quality (EXCELLENT, GOOD, AVERAGE, POOR)
     * @param {number} [data.currentTipPercentage] - Optional user's currently selected tip %
     * @returns {Promise<Object>} Smart tip response containing suggestions and context facts
     */
    getSmartTip: async (data) => {
        const response = await axiosInstance.post('/api/smart-tip', data);
        return response.data;
    },

    /**
     * Submit explicit feedback for a saved tip decision (Day 32).
     * @param {Object} data - Feedback payload
     * @param {string} data.currency - ISO currency code
     * @param {number} data.billAmount - Bill amount
     * @param {number} data.chosenTipPercentage - User's chosen percentage
     * @param {string} [data.restaurantName] - Optional restaurant name
     * @param {string} [data.serviceQuality] - Optional service quality rating
     * @param {number} [data.suggestedTipPercentage] - The percentage suggested by assistant (if applied)
     * @param {string} [data.suggestedRecommendationType] - Recommendation type
     * @returns {Promise<Object>} Feedback classification response
     */
    submitFeedback: async (data) => {
        const response = await axiosInstance.post('/api/smart-tip/feedback', data);
        return response.data;
    },

    /**
     * Get behavioral feedback summary for a currency (Day 32).
     * @param {string} currency - ISO currency code
     * @returns {Promise<Object>} Summary of counts, average difference, direction, and adaptation status
     */
    getFeedbackSummary: async (currency) => {
        const response = await axiosInstance.get('/api/smart-tip/feedback', {
            params: { currency }
        });
        return response.data;
    },

    /**
     * Get decision memory summary and recent decisions (Day 33).
     * @param {string} currency - ISO currency code
     * @returns {Promise<Object>} Summary and recent decisions array
     */
    getDecisionMemory: async (currency) => {
        const response = await axiosInstance.get('/api/smart-tip/decision-memory', {
            params: { currency }
        });
        return response.data;
    },

    /**
     * Run a what-if simulation (Day 34).
     * @param {Object} data - Simulation request payload
     * @param {string} data.currency - ISO currency code
     * @param {number} data.billAmount - Bill amount
     * @param {number} data.tipPercentage - Simulated tip percentage
     * @param {string} [data.restaurantName] - Optional restaurant name
     * @param {string} [data.serviceQuality] - Optional service quality rating
     * @returns {Promise<Object>} Simulation response
     */
    simulate: async (data) => {
        const response = await axiosInstance.post('/api/smart-tip/simulate', data);
        return response.data;
    },

    /**
     * Get learning insights and personalized decision trends (Day 35).
     * @param {string} currency - ISO currency code
     * @returns {Promise<Object>} Learning insights response with feedback summary,
     *   learning strength, preference direction, personalization effect, recent decisions, and AI explanation
     */
    getLearningInsights: async (currency) => {
        const response = await axiosInstance.get('/api/smart-tip/learning-insights', {
            params: { currency }
        });
        return response.data;
    },

    /**
     * Get user's personalization settings for a currency (Day 36).
     * @param {string} currency - ISO currency code
     * @returns {Promise<Object>} Personalization settings (enabled state, feedback count, etc.)
     */
    getPersonalizationSettings: async (currency) => {
        const response = await axiosInstance.get('/api/smart-tip/personalization', {
            params: { currency }
        });
        return response.data;
    },

    /**
     * Enable or disable personalization for a currency (Day 36).
     * @param {Object} data - Payload
     * @param {string} data.currency - ISO currency code
     * @param {boolean} data.enabled - Whether personalization is enabled
     * @returns {Promise<Object>} Updated personalization settings
     */
    updatePersonalizationSettings: async (data) => {
        const response = await axiosInstance.put('/api/smart-tip/personalization', data);
        return response.data;
    },

    /**
     * Reset learned preferences and feedback for a currency (Day 36).
     * @param {Object} data - Payload
     * @param {string} data.currency - ISO currency code
     * @returns {Promise<Object>} Reset response with deleted feedback count
     */
    resetPersonalizationLearning: async (data) => {
        const response = await axiosInstance.post('/api/smart-tip/personalization/reset', data);
        return response.data;
    },

    /**
     * Get personalization effectiveness metrics for a currency (Day 37).
     * @param {string} currency - ISO currency code
     * @returns {Promise<Object>} Effectiveness metrics, alignment rate, and calibration status
     */
    getPersonalizationEffectiveness: async (currency) => {
        const response = await axiosInstance.get('/api/smart-tip/personalization/effectiveness', {
            params: { currency }
        });
        return response.data;
    }
};

export default smartTipApi;

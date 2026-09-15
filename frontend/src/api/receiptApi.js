import axiosInstance from './axios';

/**
 * Receipt OCR API module
 *
 * PURPOSE:
 * Handles file uploads to extract receipt information via Gemini Vision.
 */
const receiptApi = {
  /**
   * Upload a receipt image for OCR analysis.
   * 
   * @param {File} file - The image file to analyze (JPEG, PNG, WebP)
   * @returns {Promise<Object>} - Extract {billAmount, restaurantName, currency}
   */
  analyzeReceipt: async (file) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await axiosInstance.post('/api/receipts/analyze', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    return response.data;
  }
};

export default receiptApi;
